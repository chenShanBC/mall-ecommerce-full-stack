package com.mallfei.stock.application.service;

import com.mallfei.common.api.PageResult;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.stock.application.dto.StockAdjustRequest;
import com.mallfei.stock.application.dto.StockHealthView;
import com.mallfei.stock.application.dto.StockOperationRequest;
import com.mallfei.stock.application.dto.StockOperationResult;
import com.mallfei.stock.application.dto.StockQuery;
import com.mallfei.stock.application.dto.StockSyncEvent;
import com.mallfei.stock.application.dto.StockView;
import com.mallfei.stock.domain.model.Stock;
import com.mallfei.stock.domain.model.StockLockRecord;
import com.mallfei.stock.domain.service.StockDomainService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class StockApplicationService {
    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = script("local a=tonumber(redis.call('GET',KEYS[1]) or '-1') local q=tonumber(ARGV[1]) if a<0 then return -2 end local s=redis.call('HGET',KEYS[4],'status') if s=='RESERVED' then return 2 end if s=='CANCELLED' or s=='CONFIRMED' then return -3 end if a<q then return -1 end redis.call('DECRBY',KEYS[1],q) redis.call('INCRBY',KEYS[2],q) redis.call('HSET',KEYS[4],'status','RESERVED','quantity',q,'lockNo',ARGV[2]) return 1");
    private static final DefaultRedisScript<Long> CANCEL_SCRIPT = script("local q=tonumber(redis.call('HGET',KEYS[3],'quantity') or '-1') local s=redis.call('HGET',KEYS[3],'status') if q<0 then return -2 end if s=='CANCELLED' then return 2 end if s=='CONFIRMED' then return -3 end if s~='RESERVED' then return -1 end redis.call('INCRBY',KEYS[1],q) redis.call('DECRBY',KEYS[2],q) redis.call('HSET',KEYS[3],'status','CANCELLED') return 1");
    private static final DefaultRedisScript<Long> CONFIRM_SCRIPT = script("local q=tonumber(redis.call('HGET',KEYS[3],'quantity') or '-1') local s=redis.call('HGET',KEYS[3],'status') if q<0 then return -2 end if s=='CONFIRMED' then return 2 end if s=='CANCELLED' then return -3 end if s~='RESERVED' then return -1 end redis.call('DECRBY',KEYS[1],q) redis.call('DECRBY',KEYS[2],q) redis.call('HSET',KEYS[3],'status','CONFIRMED') return 1");
    private final StockDomainService stockDomainService;
    private final StringRedisTemplate redisTemplate;
    private final StockEventPublisher stockEventPublisher;
    private final StockOperationLogApplicationService stockOperationLogApplicationService;

    public StockApplicationService(StockDomainService stockDomainService, StringRedisTemplate redisTemplate, StockEventPublisher stockEventPublisher, StockOperationLogApplicationService stockOperationLogApplicationService) {
        this.stockDomainService = stockDomainService;
        this.redisTemplate = redisTemplate;
        this.stockEventPublisher = stockEventPublisher;
        this.stockOperationLogApplicationService = stockOperationLogApplicationService;
    }

    public StockHealthView health() { return new StockHealthView("stock", "ready", "redis-reservation-async-db-sync"); }
    public StockView stockOf(Long skuId) { ensureLoaded(skuId); return toView(stockDomainService.loadStock(skuId), "REDIS"); }
    public PageResult<StockView> stockList(StockQuery query) { PageResult<Stock> result = stockDomainService.searchStockPage(query); return new PageResult<>(result.page(), result.size(), result.total(), result.pages(), result.records().stream().peek(stock -> ensureLoaded(stock.skuId())).map(stock -> toView(stockDomainService.loadStock(stock.skuId()), "REDIS")).toList()); }
    public long countByWarningStatus(String warningStatus) { return stockDomainService.countByWarningStatus(warningStatus); }
    public List<StockView> stockListBySkuIds(List<Long> skuIds) { return stockDomainService.loadStocksBySkuIds(skuIds).stream().peek(stock -> ensureLoaded(stock.skuId())).map(stock -> toView(stockDomainService.loadStock(stock.skuId()), "REDIS")).toList(); }
    public StockView initStock(Long skuId, Integer initialStock) { Stock before = stockDomainService.loadStocksBySkuIds(List.of(skuId)).stream().findFirst().orElse(null); Stock after = stockDomainService.initStock(skuId, initialStock); writeRedis(after); if (before == null) stockOperationLogApplicationService.record(skuId, "INIT", "PRODUCT", String.valueOf(skuId), initialStock, null, after, "初始化库存", "PRODUCT_EVENT"); return toView(after, "INITIALIZED"); }
    public StockView updateStockPolicy(Long skuId, String stockStatus, Integer lowStockThreshold, Integer highStockThreshold) { return updateStockPolicy(skuId, stockStatus, lowStockThreshold, highStockThreshold, "调整库存策略"); }
    public StockView updateStockPolicy(Long skuId, String stockStatus, Integer lowStockThreshold, Integer highStockThreshold, String reason) { Stock before = stockDomainService.loadStock(skuId); Stock after = stockDomainService.updatePolicy(skuId, stockStatus, lowStockThreshold, highStockThreshold); writeRedis(after); stockOperationLogApplicationService.record(skuId, "POLICY_UPDATE", "ADMIN", String.valueOf(skuId), 0, before, after, reason == null || reason.isBlank() ? "调整库存策略" : reason, "ADMIN_UI"); return toView(after, "POLICY_UPDATED"); }
    public StockView adjustStock(Long skuId, StockAdjustRequest request) { if (!request.totalStock().equals(request.availableStock() + request.lockedStock())) throw BusinessException.badRequest("总库存必须等于可用库存加锁定库存"); Stock before = stockDomainService.loadStock(skuId); Stock after = new Stock(before.id(), before.skuId(), request.totalStock(), request.lockedStock(), request.availableStock(), before.stockStatus(), before.lowStockThreshold(), before.highStockThreshold(), before.warningStatus(), before.version()).applyPolicy(before.stockStatus(), before.lowStockThreshold(), before.highStockThreshold()); stockDomainService.saveStock(after); writeRedis(after); stockOperationLogApplicationService.record(skuId, "MANUAL_ADJUST", "ADMIN", String.valueOf(skuId), after.totalStock() - before.totalStock(), before, after, request.reason() == null || request.reason().isBlank() ? "后台手工调整库存" : request.reason(), "ADMIN_UI"); return toView(after, "MANUAL_ADJUSTED"); }
    public StockView syncStock(Long skuId) { Stock stock = stockDomainService.loadStock(skuId); writeRedis(stock); return toView(stock, "SYNCED_FROM_DB"); }
    public StockOperationResult reserve(StockOperationRequest request) { List<StockOperationRequest.Item> reservedItems = new ArrayList<>(); try { for (StockOperationRequest.Item item : request.items()) { Stock stock = stockDomainService.loadStock(item.skuId()); stock.ensureSellable(); ensureLoaded(item.skuId()); String reservationKey = reservationKey(request.businessType(), request.businessNo(), item.skuId()); Long result = redisTemplate.execute(RESERVE_SCRIPT, List.of(availableKey(item.skuId()), lockedKey(item.skuId()), totalKey(item.skuId()), reservationKey), String.valueOf(item.quantity()), UUID.randomUUID().toString().replace("-", "")); stockDomainService.validateReserveResult(result, item.skuId()); persistReservedRecord(request.businessType(), request.businessNo(), item.skuId(), item.quantity(), reservationKey); publishSync(request.businessType(), request.businessNo(), item.skuId(), StockLockRecord.STATUS_RESERVED); reservedItems.add(item); } return resultView(StockLockRecord.STATUS_RESERVED, request.businessType(), request.businessNo()); } catch (Exception e) { rollbackReservedItems(request, reservedItems); throw e; } }
    public StockOperationResult cancel(StockOperationRequest request) { for (StockLockRecord record : stockDomainService.loadBusinessRecords(request.businessType(), request.businessNo())) { ensureLoaded(record.skuId()); Long result = redisTemplate.execute(CANCEL_SCRIPT, List.of(availableKey(record.skuId()), lockedKey(record.skuId()), reservationKey(record.businessType(), record.businessNo(), record.skuId()))); stockDomainService.validateCancelResult(result, record.skuId()); publishSync(record.businessType(), record.businessNo(), record.skuId(), StockLockRecord.STATUS_CANCELLED); } return resultView(StockLockRecord.STATUS_CANCELLED, request.businessType(), request.businessNo()); }
    public StockOperationResult confirm(StockOperationRequest request) { for (StockLockRecord record : stockDomainService.loadBusinessRecords(request.businessType(), request.businessNo())) { ensureLoaded(record.skuId()); Long result = redisTemplate.execute(CONFIRM_SCRIPT, List.of(totalKey(record.skuId()), lockedKey(record.skuId()), reservationKey(record.businessType(), record.businessNo(), record.skuId()))); stockDomainService.validateConfirmResult(result, record.skuId()); publishSync(record.businessType(), record.businessNo(), record.skuId(), StockLockRecord.STATUS_CONFIRMED); } return resultView(StockLockRecord.STATUS_CONFIRMED, request.businessType(), request.businessNo()); }
    public StockOperationResult restore(StockOperationRequest request) { for (StockOperationRequest.Item item : request.items()) { Stock before = stockDomainService.loadStock(item.skuId()); Stock after = before.restore(item.quantity()); stockDomainService.saveStock(after); writeRedis(after); stockOperationLogApplicationService.record(item.skuId(), "RESTORE", request.businessType(), request.businessNo(), item.quantity(), before, after, "回补库存", "ORDER_REFUND"); } return resultView("RESTORED", request.businessType(), request.businessNo()); }
    public StockOperationResult lock(StockOperationRequest request) { return reserve(request); }
    public StockOperationResult release(StockOperationRequest request) { return cancel(request); }
    public StockOperationResult deduct(StockOperationRequest request) { return confirm(request); }
    private void rollbackReservedItems(StockOperationRequest request, List<StockOperationRequest.Item> reservedItems) { if (reservedItems.isEmpty()) return; try { cancel(new StockOperationRequest(request.businessType(), request.businessNo(), reservedItems)); } catch (Exception e) { throw BusinessException.badRequest("库存预占失败且回滚异常: " + e.getMessage()); } }
    private void ensureLoaded(Long skuId) { if (Boolean.TRUE.equals(redisTemplate.hasKey(totalKey(skuId))) && Boolean.TRUE.equals(redisTemplate.hasKey(lockedKey(skuId))) && Boolean.TRUE.equals(redisTemplate.hasKey(availableKey(skuId)))) return; writeRedis(stockDomainService.loadStock(skuId)); }
    private void writeRedis(Stock stock) { redisTemplate.opsForValue().set(totalKey(stock.skuId()), String.valueOf(stock.totalStock())); redisTemplate.opsForValue().set(lockedKey(stock.skuId()), String.valueOf(stock.lockedStock())); redisTemplate.opsForValue().set(availableKey(stock.skuId()), String.valueOf(stock.availableStock())); }
    private void persistReservedRecord(String businessType, String businessNo, Long skuId, Integer quantity, String reservationKey) { StockLockRecord existing = stockDomainService.loadBusinessRecord(businessType, businessNo, skuId); stockDomainService.ensureCanCreateReservation(existing, skuId); if (existing != null && existing.reserved()) return; Object lockNo = redisTemplate.opsForHash().get(reservationKey, "lockNo"); stockDomainService.saveReservation(StockLockRecord.reserve(String.valueOf(lockNo), skuId, businessType, businessNo, quantity, LocalDateTime.now())); }
    private void publishSync(String businessType, String businessNo, Long skuId, String targetStatus) { stockEventPublisher.publishSyncEvent(new StockSyncEvent(businessType, businessNo, skuId, targetStatus)); }
    private int readInt(String key) { String value = redisTemplate.opsForValue().get(key); if (value == null) throw BusinessException.badRequest("Redis库存缓存缺失: " + key); return Integer.parseInt(value); }
    private StockView toView(Stock stock, String source) { return new StockView(stock.skuId(), readInt(totalKey(stock.skuId())), readInt(lockedKey(stock.skuId())), readInt(availableKey(stock.skuId())), stock.stockStatus(), stock.lowStockThreshold(), stock.highStockThreshold(), stock.warningStatus(), source); }
    private StockOperationResult resultView(String status, String businessType, String businessNo) { return new StockOperationResult(status, businessType, businessNo, "REDIS_RESERVATION_ASYNC_DB_SYNC"); }
    private String totalKey(Long skuId) { return "stock:total:" + skuId; }
    private String lockedKey(Long skuId) { return "stock:locked:" + skuId; }
    private String availableKey(Long skuId) { return "stock:available:" + skuId; }
    private String reservationKey(String businessType, String businessNo, Long skuId) { return "stock:reservation:" + businessType + ":" + businessNo + ":" + skuId; }
    private static DefaultRedisScript<Long> script(String text) { DefaultRedisScript<Long> script = new DefaultRedisScript<>(); script.setScriptText(text); script.setResultType(Long.class); return script; }
}
