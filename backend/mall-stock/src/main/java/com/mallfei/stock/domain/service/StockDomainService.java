package com.mallfei.stock.domain.service;

import com.mallfei.common.api.PageResult;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.stock.application.dto.StockQuery;
import com.mallfei.stock.domain.model.Stock;
import com.mallfei.stock.domain.model.StockLockRecord;
import com.mallfei.stock.domain.repository.StockLockRepository;
import com.mallfei.stock.domain.repository.StockRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockDomainService {

    private final StockRepository stockRepository;
    private final StockLockRepository stockLockRepository;

    public StockDomainService(StockRepository stockRepository,
                              StockLockRepository stockLockRepository) {
        this.stockRepository = stockRepository;
        this.stockLockRepository = stockLockRepository;
    }

    public Stock loadStock(Long skuId) { return stockRepository.findBySkuId(skuId).orElseThrow(() -> BusinessException.badRequest("库存不存在: " + skuId)); }
    public List<Stock> loadStocksBySkuIds(List<Long> skuIds) { return stockRepository.findBySkuIds(skuIds); }
    public List<Stock> loadAllStocks() { return stockRepository.findAll(); }
    public List<Stock> searchStocks(StockQuery query) { return stockRepository.search(query); }
    public PageResult<Stock> searchStockPage(StockQuery query) { return stockRepository.searchPage(query); }
    public long countByWarningStatus(String warningStatus) { return stockRepository.countByWarningStatus(warningStatus); }
    public Stock initStock(Long skuId, Integer initialStock) { return stockRepository.findBySkuId(skuId).orElseGet(() -> stockRepository.save(Stock.initialize(skuId, initialStock))); }
    public Stock updatePolicy(Long skuId, String stockStatus, Integer lowStockThreshold, Integer highStockThreshold) { Stock updated = loadStock(skuId).applyPolicy(stockStatus, lowStockThreshold, highStockThreshold); saveStock(updated); return updated; }
    public List<StockLockRecord> loadBusinessRecords(String businessType, String businessNo) { return stockLockRepository.findByBusiness(businessType, businessNo); }
    public StockLockRecord loadBusinessRecord(String businessType, String businessNo, Long skuId) { return stockLockRepository.findByBusiness(businessType, businessNo, skuId).orElse(null); }
    public long sumReservedQuantity(Long skuId) { return stockLockRepository.sumReservedQuantity(skuId); }
    public StockLockRecord saveReservation(StockLockRecord stockLockRecord) { return stockLockRepository.save(stockLockRecord); }
    public void saveStock(Stock stock) { if (stock.id() == null) { stockRepository.save(stock); return; } stockRepository.update(stock); }
    public void calibrateSnapshot(Long skuId, Integer lockedStock, Integer availableStock, String warningStatus) { stockRepository.calibrateSnapshot(skuId, lockedStock, availableStock, warningStatus); }
    public void validateReserveResult(Long result, Long skuId) { if (result == null) throw BusinessException.badRequest("Redis库存预占失败: " + skuId); if (result == -1L) throw BusinessException.badRequest("SKU库存不足: " + skuId); if (result == -2L) throw BusinessException.badRequest("SKU Redis库存未初始化: " + skuId); if (result == -3L) throw BusinessException.badRequest("当前业务单库存状态不允许重复预占: " + skuId); }
    public void validateCancelResult(Long result, Long skuId) { if (result == null) throw BusinessException.badRequest("库存取消失败: " + skuId); if (result == -3L) throw BusinessException.badRequest("库存已确认，不能取消: " + skuId); }
    public void validateConfirmResult(Long result, Long skuId) { if (result == null || result == -1L || result == -2L) throw BusinessException.badRequest("库存确认失败: " + skuId); if (result == -3L) throw BusinessException.badRequest("库存已取消，不能确认: " + skuId); }
    public void ensureCanCreateReservation(StockLockRecord existing, Long skuId) { if (existing == null) return; if (existing.reserved()) return; throw BusinessException.badRequest("库存锁记录状态不允许再次预占: " + skuId); }
}
