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
import com.mallfei.stock.application.vo.StockConsistencyCheckView;
import com.mallfei.stock.application.vo.StockConsistencySnapshotView;
import com.mallfei.stock.domain.model.Stock;
import com.mallfei.stock.domain.model.StockAdjustmentCommand;
import com.mallfei.stock.domain.model.StockAdjustmentType;
import com.mallfei.stock.domain.model.StockConsistencyCheckResult;
import com.mallfei.stock.domain.model.StockConsistencySnapshot;
import com.mallfei.stock.domain.model.StockLockRecord;
import com.mallfei.stock.domain.service.StockConsistencyDomainService;
import com.mallfei.stock.domain.service.StockDomainService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StockApplicationService {
    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = script("local a=tonumber(redis.call('GET',KEYS[1]) or '-1') local q=tonumber(ARGV[1]) if a<0 then return -2 end local s=redis.call('HGET',KEYS[4],'status') if s=='RESERVED' then return 2 end if s=='CANCELLED' or s=='CONFIRMED' then return -3 end if a<q then return -1 end redis.call('DECRBY',KEYS[1],q) redis.call('INCRBY',KEYS[2],q) redis.call('HSET',KEYS[4],'status','RESERVED','quantity',q,'lockNo',ARGV[2],'reservedAt',ARGV[3]) return 1");
    private static final DefaultRedisScript<Long> CANCEL_SCRIPT = script("local q=tonumber(redis.call('HGET',KEYS[3],'quantity') or '-1') local s=redis.call('HGET',KEYS[3],'status') if q<0 then return -2 end if s=='CANCELLED' then return 2 end if s=='CONFIRMED' then return -3 end if s~='RESERVED' then return -1 end redis.call('INCRBY',KEYS[1],q) redis.call('DECRBY',KEYS[2],q) redis.call('HSET',KEYS[3],'status','CANCELLED') return 1");
    private static final DefaultRedisScript<Long> CONFIRM_SCRIPT = script("local q=tonumber(redis.call('HGET',KEYS[3],'quantity') or '-1') local s=redis.call('HGET',KEYS[3],'status') if q<0 then return -2 end if s=='CONFIRMED' then return 2 end if s=='CANCELLED' then return -3 end if s~='RESERVED' then return -1 end redis.call('DECRBY',KEYS[1],q) redis.call('DECRBY',KEYS[2],q) redis.call('HSET',KEYS[3],'status','CONFIRMED') return 1");
    private final StockDomainService stockDomainService;
    private final StringRedisTemplate redisTemplate;
    private final StockEventPublisher stockEventPublisher;
    private final StockOperationLogApplicationService stockOperationLogApplicationService;
    private final com.mallfei.stock.domain.repository.StockLockRepository stockLockRepository;
    private final com.mallfei.stock.domain.service.StockPersistenceDomainService stockPersistenceDomainService;
    private final StockPersistenceService stockPersistenceService;
    private final StockConsistencyDomainService stockConsistencyDomainService;
    private final com.mallfei.stock.domain.repository.StockReconciliationRecordRepository stockReconciliationRecordRepository;

    public StockApplicationService(StockDomainService stockDomainService, StringRedisTemplate redisTemplate, StockEventPublisher stockEventPublisher, StockOperationLogApplicationService stockOperationLogApplicationService, com.mallfei.stock.domain.repository.StockLockRepository stockLockRepository, com.mallfei.stock.domain.service.StockPersistenceDomainService stockPersistenceDomainService, StockPersistenceService stockPersistenceService, StockConsistencyDomainService stockConsistencyDomainService, com.mallfei.stock.domain.repository.StockReconciliationRecordRepository stockReconciliationRecordRepository) {
        this.stockDomainService = stockDomainService;
        this.redisTemplate = redisTemplate;
        this.stockEventPublisher = stockEventPublisher;
        this.stockOperationLogApplicationService = stockOperationLogApplicationService;
        this.stockLockRepository = stockLockRepository;
        this.stockPersistenceDomainService = stockPersistenceDomainService;
        this.stockPersistenceService = stockPersistenceService;
        this.stockConsistencyDomainService = stockConsistencyDomainService;
        this.stockReconciliationRecordRepository = stockReconciliationRecordRepository;
    }

    public StockHealthView health() { return new StockHealthView("stock", "ready", "redis-reservation-async-db-sync"); }
    public StockView stockOf(Long skuId) { ensureLoaded(skuId); return toView(stockDomainService.loadStock(skuId), "REDIS"); }
    public PageResult<StockView> stockList(StockQuery query) { PageResult<Stock> result = stockDomainService.searchStockPage(query); return new PageResult<>(result.page(), result.size(), result.total(), result.pages(), result.records().stream().peek(stock -> ensureLoaded(stock.skuId())).map(stock -> toView(stockDomainService.loadStock(stock.skuId()), "REDIS")).toList()); }
    public long countByWarningStatus(String warningStatus) { return stockDomainService.countByWarningStatus(warningStatus); }
    public List<StockView> stockListBySkuIds(List<Long> skuIds) { return stockDomainService.loadStocksBySkuIds(skuIds).stream().peek(stock -> ensureLoaded(stock.skuId())).map(stock -> toView(stockDomainService.loadStock(stock.skuId()), "REDIS")).toList(); }
    public StockView initStock(Long skuId, Integer initialStock) { Stock before = stockDomainService.loadStocksBySkuIds(List.of(skuId)).stream().findFirst().orElse(null); Stock after = stockDomainService.initStock(skuId, initialStock); writeRedis(after); if (before == null) stockOperationLogApplicationService.record(skuId, "INIT", "PRODUCT", String.valueOf(skuId), initialStock, null, after, "初始化库存", "PRODUCT_EVENT"); return toView(after, "INITIALIZED"); }
    public StockView updateStockPolicy(Long skuId, String stockStatus, Integer lowStockThreshold, Integer highStockThreshold) { return updateStockPolicy(skuId, stockStatus, lowStockThreshold, highStockThreshold, "调整库存策略"); }
    public StockView updateStockPolicy(Long skuId, String stockStatus, Integer lowStockThreshold, Integer highStockThreshold, String reason) { Stock before = stockDomainService.loadStock(skuId); Stock after = stockDomainService.updatePolicy(skuId, stockStatus, lowStockThreshold, highStockThreshold); writeRedis(after); stockOperationLogApplicationService.record(skuId, "POLICY_UPDATE", "ADMIN", String.valueOf(skuId), 0, before, after, reason == null || reason.isBlank() ? "调整库存策略" : reason, "ADMIN_UI"); return toView(after, "POLICY_UPDATED"); }
    public StockView adjustStock(Long skuId, StockAdjustRequest request) { Stock before = stockDomainService.loadStock(skuId); StockAdjustmentCommand command = adjustmentCommand(request); Stock after = before.adjust(command); stockDomainService.saveStock(after); writeRedis(after); stockOperationLogApplicationService.record(skuId, "MANUAL_ADJUST", "ADMIN", "MANUAL_ADJUST_" + java.util.UUID.randomUUID().toString().replace("-", ""), after.totalStock() - before.totalStock(), before, after, command.auditRemark(), "ADMIN_UI"); return toView(after, "MANUAL_ADJUSTED"); }
    public StockView syncStock(Long skuId) { Stock stock = recoverRedisStock(stockDomainService.loadStock(skuId)); writeRedis(stock); return toView(stock, "SYNCED_FROM_DB"); }
    public StockConsistencyCheckView checkConsistency(Long skuId) {
        StockConsistencyCheckResult result = calculateConsistency(skuId);
        com.mallfei.stock.domain.model.StockReconciliationRecord saved = stockReconciliationRecordRepository.save(com.mallfei.stock.domain.model.StockReconciliationRecord.fromCheck(result));
        return toConsistencyView(saved);
    }
    public com.mallfei.stock.domain.model.StockReconciliationRecord createReconciliationRecordForJob(Long skuId, boolean onlyRecordInconsistent, boolean skipPendingInconsistent) { StockConsistencyCheckResult result = calculateConsistency(skuId); if (result.consistent() && onlyRecordInconsistent) return null; if (result.consistent() && !onlyRecordInconsistent) return stockReconciliationRecordRepository.save(com.mallfei.stock.domain.model.StockReconciliationRecord.fromCheck(result)); if (skipPendingInconsistent && stockReconciliationRecordRepository.existsPendingInconsistent(skuId)) return null; return stockReconciliationRecordRepository.save(com.mallfei.stock.domain.model.StockReconciliationRecord.fromCheck(result)); }
    public com.mallfei.common.api.PageResult<com.mallfei.stock.application.vo.StockReconciliationRecordView> pageReconciliationRecords(Long skuId, String status, long page, long size, String sortBy, String sortOrder) { var result = stockReconciliationRecordRepository.page(skuId, status, page, size, sortBy, sortOrder); return new com.mallfei.common.api.PageResult<>(result.page(), result.size(), result.total(), result.pages(), result.records().stream().map(this::toReconciliationView).toList()); }
    public com.mallfei.stock.application.vo.StockReconciliationRecordView reconciliationRecord(Long id) { return toReconciliationView(loadReconciliationRecord(id)); }
    public com.mallfei.stock.application.vo.StockReconciliationRecordView repairReconciliationRecord(Long id, String remark) { com.mallfei.stock.domain.model.StockReconciliationRecord record = loadReconciliationRecord(id); StockConsistencyCheckResult latest = calculateConsistency(record.skuId()); if (!latest.consistent()) { Stock stock = stockDomainService.loadStock(record.skuId()); StockConsistencySnapshot expected = latest.expectedSnapshot(); stockDomainService.calibrateSnapshot(record.skuId(), expected.lockedStock(), expected.availableStock(), warningStatus(expected.availableStock(), stock.lowStockThreshold(), stock.highStockThreshold())); writeRedis(new Stock(stock.id(), record.skuId(), expected.totalStock(), expected.lockedStock(), expected.availableStock(), stock.stockStatus(), stock.lowStockThreshold(), stock.highStockThreshold(), warningStatus(expected.availableStock(), stock.lowStockThreshold(), stock.highStockThreshold()), stock.version())); }
        com.mallfei.stock.domain.model.StockReconciliationRecord repaired = record.repaired(remark); stockReconciliationRecordRepository.update(repaired); return toReconciliationView(repaired); }
    public com.mallfei.stock.application.vo.StockReconciliationRecordView ignoreReconciliationRecord(Long id, String remark) { com.mallfei.stock.domain.model.StockReconciliationRecord ignored = loadReconciliationRecord(id).ignored(remark); stockReconciliationRecordRepository.update(ignored); return toReconciliationView(ignored); }
    public StockOperationResult reserve(StockOperationRequest request) { List<StockOperationRequest.Item> items = mergeItems(request.items()); Map<Long, Stock> stocks = loadStocks(items); List<StockOperationRequest.Item> reservedItems = new ArrayList<>(); try { for (StockOperationRequest.Item item : items) { Stock stock = stocks.get(item.skuId()); stock.ensureSellable(); ensureLoaded(stock); Stock before = stockFromRedis(item.skuId(), stock); String reservationKey = reservationKey(request.businessType(), request.businessNo(), item.skuId()); Long result = redisTemplate.execute(RESERVE_SCRIPT, List.of(availableKey(item.skuId()), lockedKey(item.skuId()), totalKey(item.skuId()), reservationKey), String.valueOf(item.quantity()), UUID.randomUUID().toString().replace("-", ""), String.valueOf(System.currentTimeMillis())); stockDomainService.validateReserveResult(result, item.skuId()); boolean newlyReserved = result == 1L; if (newlyReserved) reservedItems.add(item); Stock after = stockFromRedis(item.skuId(), stock); persistReservedRecord(request.businessType(), request.businessNo(), item.skuId(), item.quantity(), reservationKey); stockOperationLogApplicationService.recordIfAbsent(item.skuId(), "RESERVE", request.businessType(), request.businessNo(), item.quantity(), before, after, "预占库存", "ORDER_EVENT"); if (newlyReserved) publishSync(request.businessType(), request.businessNo(), item.skuId(), StockLockRecord.STATUS_RESERVED); } return resultView(StockLockRecord.STATUS_RESERVED, request.businessType(), request.businessNo()); } catch (Exception e) { rollbackReservedItems(request, reservedItems); throw e; } }
    public StockOperationResult cancel(StockOperationRequest request) { return cancelBusiness(request.businessType(), request.businessNo()); }
    public StockOperationResult cancelBusiness(String businessType, String businessNo) { for (StockLockRecord record : stockDomainService.loadBusinessRecords(businessType, businessNo)) { if (!record.reserved()) continue; ensureLoaded(record.skuId()); ensureReservationLoaded(record); Stock persisted = stockDomainService.loadStock(record.skuId()); Stock before = stockFromRedis(record.skuId(), persisted); Long result = redisTemplate.execute(CANCEL_SCRIPT, List.of(availableKey(record.skuId()), lockedKey(record.skuId()), reservationKey(record.businessType(), record.businessNo(), record.skuId()))); stockDomainService.validateCancelResult(result, record.skuId()); Stock after = stockFromRedis(record.skuId(), persisted); stockOperationLogApplicationService.recordIfAbsent(record.skuId(), "CANCEL_RESERVE", record.businessType(), record.businessNo(), record.quantity(), before, after, "释放预占库存", "ORDER_EVENT"); if (redisTerminalSynced(result)) syncTerminalStock(record.businessType(), record.businessNo(), record.skuId(), StockLockRecord.STATUS_CANCELLED); } return resultView(StockLockRecord.STATUS_CANCELLED, businessType, businessNo); }
    public StockOperationResult confirm(StockOperationRequest request) { for (StockLockRecord record : stockDomainService.loadBusinessRecords(request.businessType(), request.businessNo())) { ensureLoaded(record.skuId()); ensureReservationLoaded(record); Stock persisted = stockDomainService.loadStock(record.skuId()); Stock before = stockFromRedis(record.skuId(), persisted); Long result = redisTemplate.execute(CONFIRM_SCRIPT, List.of(totalKey(record.skuId()), lockedKey(record.skuId()), reservationKey(record.businessType(), record.businessNo(), record.skuId()))); stockDomainService.validateConfirmResult(result, record.skuId()); Stock after = stockFromRedis(record.skuId(), persisted); stockOperationLogApplicationService.recordIfAbsent(record.skuId(), "CONFIRM_DEDUCT", record.businessType(), record.businessNo(), -record.quantity(), before, after, "确认扣减库存", "ORDER_EVENT"); if (redisTerminalSynced(result)) syncTerminalStock(record.businessType(), record.businessNo(), record.skuId(), StockLockRecord.STATUS_CONFIRMED); } return resultView(StockLockRecord.STATUS_CONFIRMED, request.businessType(), request.businessNo()); }
    public StockOperationResult restore(StockOperationRequest request) { for (StockOperationRequest.Item item : request.items()) { Stock before = stockDomainService.loadStock(item.skuId()); Stock after = before.restore(item.quantity()); stockDomainService.saveStock(after); writeRedis(after); stockOperationLogApplicationService.record(item.skuId(), "RESTORE", request.businessType(), request.businessNo(), item.quantity(), before, after, "回补库存", "ORDER_REFUND"); } return resultView("RESTORED", request.businessType(), request.businessNo()); }
    public StockOperationResult lock(StockOperationRequest request) { return reserve(request); }
    public StockOperationResult release(StockOperationRequest request) { return cancel(request); }
    public StockOperationResult deduct(StockOperationRequest request) { return confirm(request); }
    private void rollbackReservedItems(StockOperationRequest request, List<StockOperationRequest.Item> reservedItems) { if (reservedItems.isEmpty()) return; try { cancel(new StockOperationRequest(request.businessType(), request.businessNo(), reservedItems)); } catch (Exception e) { throw BusinessException.badRequest("库存预占失败且回滚异常: " + e.getMessage()); } }
    private StockAdjustmentCommand adjustmentCommand(StockAdjustRequest request) { StockAdjustmentType type = StockAdjustmentType.from(request.adjustmentType()); if (type == StockAdjustmentType.OTHER && request.changeQuantity() == null) return StockAdjustmentCommand.direct(request.totalStock(), request.availableStock(), request.lockedStock(), request.reason()); return StockAdjustmentCommand.business(type, request.changeQuantity(), request.reason(), request.remark()); }
    private List<StockOperationRequest.Item> mergeItems(List<StockOperationRequest.Item> items) { if (items == null || items.isEmpty()) return List.of(); return items.stream().collect(Collectors.groupingBy(StockOperationRequest.Item::skuId, Collectors.summingInt(item -> item.quantity() == null ? 0 : item.quantity()))).entrySet().stream().map(entry -> new StockOperationRequest.Item(entry.getKey(), entry.getValue())).toList(); }
    private Map<Long, Stock> loadStocks(List<StockOperationRequest.Item> items) { List<Long> skuIds = items.stream().map(StockOperationRequest.Item::skuId).distinct().toList(); Map<Long, Stock> stocks = stockDomainService.loadStocksBySkuIds(skuIds).stream().collect(Collectors.toMap(Stock::skuId, Function.identity())); for (Long skuId : skuIds) { if (!stocks.containsKey(skuId)) throw BusinessException.badRequest("库存不存在: " + skuId); } return stocks; }
    private void ensureLoaded(Long skuId) { if (Boolean.TRUE.equals(redisTemplate.hasKey(totalKey(skuId))) && Boolean.TRUE.equals(redisTemplate.hasKey(lockedKey(skuId))) && Boolean.TRUE.equals(redisTemplate.hasKey(availableKey(skuId)))) return; writeRedis(recoverRedisStock(stockDomainService.loadStock(skuId))); }
    private void ensureLoaded(Stock stock) { if (Boolean.TRUE.equals(redisTemplate.hasKey(totalKey(stock.skuId()))) && Boolean.TRUE.equals(redisTemplate.hasKey(lockedKey(stock.skuId()))) && Boolean.TRUE.equals(redisTemplate.hasKey(availableKey(stock.skuId())))) return; writeRedis(recoverRedisStock(stock)); }
    private void ensureReservationLoaded(StockLockRecord record) { String key = reservationKey(record.businessType(), record.businessNo(), record.skuId()); if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) return; if (!record.reserved()) return; redisTemplate.opsForHash().putAll(key, Map.of("status", StockLockRecord.STATUS_RESERVED, "quantity", String.valueOf(record.quantity()), "lockNo", record.lockNo(), "reservedAt", String.valueOf(System.currentTimeMillis()))); }
    private void writeRedis(Stock stock) { redisTemplate.opsForValue().set(totalKey(stock.skuId()), String.valueOf(stock.totalStock())); redisTemplate.opsForValue().set(lockedKey(stock.skuId()), String.valueOf(stock.lockedStock())); redisTemplate.opsForValue().set(availableKey(stock.skuId()), String.valueOf(stock.availableStock())); }
    private Stock recoverRedisStock(Stock stock) { long unsyncedReservedQuantity = stockLockRepository.sumUnpersistedReservedQuantity(stock.skuId()); com.mallfei.stock.domain.service.StockPersistenceDomainService.RedisRecoveryPlan plan = stockPersistenceDomainService.planRedisRecovery(stock, unsyncedReservedQuantity); return new Stock(stock.id(), stock.skuId(), plan.totalStock(), plan.lockedStock(), plan.availableStock(), stock.stockStatus(), stock.lowStockThreshold(), stock.highStockThreshold(), warningStatus(plan.availableStock(), stock.lowStockThreshold(), stock.highStockThreshold()), stock.version()); }
    private Stock stockFromRedis(Long skuId, Stock persisted) { int total = readInt(totalKey(skuId)); int locked = readInt(lockedKey(skuId)); int available = readInt(availableKey(skuId)); return new Stock(persisted.id(), skuId, total, locked, available, persisted.stockStatus(), persisted.lowStockThreshold(), persisted.highStockThreshold(), warningStatus(available, persisted.lowStockThreshold(), persisted.highStockThreshold()), persisted.version()); }
    private String warningStatus(int availableStock, Integer lowStockThreshold, Integer highStockThreshold) { int low = Math.max(0, lowStockThreshold == null ? 0 : lowStockThreshold); int high = Math.max(low, highStockThreshold == null ? low : highStockThreshold); if (availableStock <= low) return Stock.WARNING_LOW; if (availableStock >= high) return Stock.WARNING_HIGH; return Stock.WARNING_NORMAL; }
    private void persistReservedRecord(String businessType, String businessNo, Long skuId, Integer quantity, String reservationKey) { StockLockRecord existing = stockDomainService.loadBusinessRecord(businessType, businessNo, skuId); stockDomainService.ensureCanCreateReservation(existing, skuId); if (existing != null && existing.reserved()) return; Object lockNo = redisTemplate.opsForHash().get(reservationKey, "lockNo"); stockDomainService.saveReservation(StockLockRecord.reserve(String.valueOf(lockNo), skuId, businessType, businessNo, quantity, LocalDateTime.now())); }
    private void publishSync(String businessType, String businessNo, Long skuId, String targetStatus) { stockEventPublisher.publishSyncEvent(new StockSyncEvent(businessType, businessNo, skuId, targetStatus)); }
    private void syncTerminalStock(String businessType, String businessNo, Long skuId, String targetStatus) { stockPersistenceService.syncReservation(businessType, businessNo, skuId, targetStatus); publishSync(businessType, businessNo, skuId, targetStatus); }
    private boolean redisTerminalSynced(Long scriptResult) { return scriptResult != null && (scriptResult == 1L || scriptResult == 2L); }
    private int readInt(String key) { String value = redisTemplate.opsForValue().get(key); if (value == null) throw BusinessException.badRequest("Redis库存缓存缺失: " + key); return Integer.parseInt(value); }
    private Integer readNullableInt(String key) { String value = redisTemplate.opsForValue().get(key); return value == null ? null : Integer.parseInt(value); }
    private StockView toView(Stock stock, String source) { return new StockView(stock.skuId(), readInt(totalKey(stock.skuId())), readInt(lockedKey(stock.skuId())), readInt(availableKey(stock.skuId())), stock.stockStatus(), stock.lowStockThreshold(), stock.highStockThreshold(), stock.warningStatus(), source); }
    private com.mallfei.stock.domain.model.StockReconciliationRecord createReconciliationRecord(Long skuId) { return stockReconciliationRecordRepository.save(com.mallfei.stock.domain.model.StockReconciliationRecord.fromCheck(calculateConsistency(skuId))); }
    private StockConsistencyCheckResult calculateConsistency(Long skuId) { Stock stock = stockDomainService.loadStock(skuId); Integer redisTotal = readNullableInt(totalKey(skuId)); Integer redisLocked = readNullableInt(lockedKey(skuId)); Integer redisAvailable = readNullableInt(availableKey(skuId)); StockConsistencySnapshot redisSnapshot = redisTotal == null || redisLocked == null || redisAvailable == null ? null : StockConsistencySnapshot.of(redisTotal, redisLocked, redisAvailable, "REDIS"); return stockConsistencyDomainService.check(stock, stockDomainService.sumReservedQuantity(skuId), redisSnapshot); }
    private com.mallfei.stock.domain.model.StockReconciliationRecord refreshReconciliationRecord(com.mallfei.stock.domain.model.StockReconciliationRecord existing, com.mallfei.stock.domain.model.StockReconciliationRecord checked) {
        com.mallfei.stock.domain.model.StockReconciliationRecord refreshed = new com.mallfei.stock.domain.model.StockReconciliationRecord(
                existing.id(),
                existing.skuId(),
                checked.status(),
                checked.severity(),
                checked.stockSnapshot(),
                checked.expectedSnapshot(),
                checked.redisSnapshot(),
                checked.differences(),
                checked.repairStatus(),
                null,
                checked.checkedAt(),
                null,
                existing.createdAt(),
                java.time.LocalDateTime.now()
        );
        stockReconciliationRecordRepository.update(refreshed);
        return refreshed;
    }
    private com.mallfei.stock.domain.model.StockReconciliationRecord loadReconciliationRecord(Long id) { return stockReconciliationRecordRepository.findById(id).orElseThrow(() -> BusinessException.badRequest("库存对账记录不存在: " + id)); }
    private StockConsistencyCheckView toConsistencyView(com.mallfei.stock.domain.model.StockReconciliationRecord record) { return new StockConsistencyCheckView(record.skuId(), com.mallfei.stock.domain.model.StockReconciliationRecord.STATUS_CONSISTENT.equals(record.status()), record.status(), record.severity(), toSnapshotView(record.stockSnapshot()), toSnapshotView(record.expectedSnapshot()), toSnapshotView(record.redisSnapshot()), record.differences(), record.checkedAt()); }
    private StockConsistencyCheckView toConsistencyView(StockConsistencyCheckResult result) { return new StockConsistencyCheckView(result.skuId(), result.consistent(), result.status(), result.severity(), toSnapshotView(result.stockSnapshot()), toSnapshotView(result.expectedSnapshot()), toSnapshotView(result.redisSnapshot()), result.differences(), result.checkedAt()); }
    private com.mallfei.stock.application.vo.StockReconciliationRecordView toReconciliationView(com.mallfei.stock.domain.model.StockReconciliationRecord record) { return new com.mallfei.stock.application.vo.StockReconciliationRecordView(record.id(), record.skuId(), record.status(), record.severity(), toSnapshotView(record.stockSnapshot()), toSnapshotView(record.expectedSnapshot()), toSnapshotView(record.redisSnapshot()), record.differences(), record.repairStatus(), record.repairRemark(), record.checkedAt(), record.repairedAt(), record.createdAt(), record.updatedAt()); }
    private StockConsistencySnapshotView toSnapshotView(StockConsistencySnapshot snapshot) { return snapshot == null ? null : new StockConsistencySnapshotView(snapshot.totalStock(), snapshot.lockedStock(), snapshot.availableStock(), snapshot.source()); }
    private StockOperationResult resultView(String status, String businessType, String businessNo) { return new StockOperationResult(status, businessType, businessNo, "REDIS_RESERVATION_ASYNC_DB_SYNC"); }
    private String totalKey(Long skuId) { return "stock:total:" + skuId; }
    private String lockedKey(Long skuId) { return "stock:locked:" + skuId; }
    private String availableKey(Long skuId) { return "stock:available:" + skuId; }
    private String reservationKey(String businessType, String businessNo, Long skuId) { return "stock:reservation:" + businessType + ":" + businessNo + ":" + skuId; }
    private static DefaultRedisScript<Long> script(String text) { DefaultRedisScript<Long> script = new DefaultRedisScript<>(); script.setScriptText(text); script.setResultType(Long.class); return script; }
}
