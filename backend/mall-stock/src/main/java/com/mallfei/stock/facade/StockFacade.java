package com.mallfei.stock.facade;

import com.mallfei.common.api.PageResult;
import com.mallfei.stock.application.dto.StockAdjustRequest;
import com.mallfei.stock.application.dto.StockOperationRequest;
import com.mallfei.stock.application.dto.StockOperationResult;
import com.mallfei.stock.application.dto.StockQuery;
import com.mallfei.stock.application.dto.StockView;
import com.mallfei.stock.application.service.StockApplicationService;
import com.mallfei.stock.application.service.StockOperationLogApplicationService;
import com.mallfei.stock.application.vo.StockConsistencyCheckView;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class StockFacade {

    private final StockApplicationService stockApplicationService;
    private final StockOperationLogApplicationService stockOperationLogApplicationService;

    public StockFacade(StockApplicationService stockApplicationService,
                       StockOperationLogApplicationService stockOperationLogApplicationService) {
        this.stockApplicationService = stockApplicationService;
        this.stockOperationLogApplicationService = stockOperationLogApplicationService;
    }

    public StockSnapshot stockOf(Long skuId) { return toSnapshot(stockApplicationService.stockOf(skuId)); }
    public PageResult<StockSnapshot> stockList(StockQuery query) { var result = stockApplicationService.stockList(query); return new PageResult<>(result.page(), result.size(), result.total(), result.pages(), result.records().stream().map(this::toSnapshot).toList()); }
    public long countByWarningStatus(String warningStatus) { return stockApplicationService.countByWarningStatus(warningStatus); }
    public List<StockSnapshot> stockListBySkuIds(List<Long> skuIds) { return stockApplicationService.stockListBySkuIds(skuIds).stream().map(this::toSnapshot).toList(); }
    public PageResult<StockOperationLogSnapshot> pageLogs(Long skuId, String operationType, LocalDateTime startTime, LocalDateTime endTime, long page, long size) { var result = stockOperationLogApplicationService.pageLogs(skuId, operationType, startTime, endTime, page, size); return new PageResult<>(result.page(), result.size(), result.total(), result.pages(), result.records().stream().map(this::toSnapshot).toList()); }
    public StockSnapshot initStock(Long skuId, Integer initialStock) { return toSnapshot(stockApplicationService.initStock(skuId, initialStock)); }
    public StockSnapshot updateStockPolicy(Long skuId, String stockStatus, Integer lowStockThreshold, Integer highStockThreshold) { return toSnapshot(stockApplicationService.updateStockPolicy(skuId, stockStatus, lowStockThreshold, highStockThreshold)); }
    public StockSnapshot updateStockPolicy(Long skuId, String stockStatus, Integer lowStockThreshold, Integer highStockThreshold, String reason) { return toSnapshot(stockApplicationService.updateStockPolicy(skuId, stockStatus, lowStockThreshold, highStockThreshold, reason)); }
    public StockSnapshot adjustStock(Long skuId, StockAdjustRequest request) { return toSnapshot(stockApplicationService.adjustStock(skuId, request)); }
    public StockSnapshot syncStock(Long skuId) { return toSnapshot(stockApplicationService.syncStock(skuId)); }
    public StockConsistencyCheckView checkConsistency(Long skuId) { return stockApplicationService.checkConsistency(skuId); }
    public PageResult<com.mallfei.stock.application.vo.StockReconciliationRecordView> pageReconciliationRecords(Long skuId, String status, long page, long size, String sortBy, String sortOrder) { return stockApplicationService.pageReconciliationRecords(skuId, status, page, size, sortBy, sortOrder); }
    public com.mallfei.stock.application.vo.StockReconciliationRecordView reconciliationRecord(Long id) { return stockApplicationService.reconciliationRecord(id); }
    public com.mallfei.stock.application.vo.StockReconciliationRecordView repairReconciliationRecord(Long id, String remark) { return stockApplicationService.repairReconciliationRecord(id, remark); }
    public com.mallfei.stock.application.vo.StockReconciliationRecordView ignoreReconciliationRecord(Long id, String remark) { return stockApplicationService.ignoreReconciliationRecord(id, remark); }
    public StockOperationResultSnapshot reserve(StockOperationRequest request) { return toOperationSnapshot(stockApplicationService.reserve(request)); }
    public StockOperationResultSnapshot cancel(StockOperationRequest request) { return toOperationSnapshot(stockApplicationService.cancel(request)); }
    public StockOperationResultSnapshot confirm(StockOperationRequest request) { return toOperationSnapshot(stockApplicationService.confirm(request)); }
    public StockOperationResultSnapshot restore(StockOperationRequest request) { return toOperationSnapshot(stockApplicationService.restore(request)); }
    public StockOperationResultSnapshot lock(StockOperationRequest request) { return toOperationSnapshot(stockApplicationService.lock(request)); }
    public StockOperationResultSnapshot release(StockOperationRequest request) { return toOperationSnapshot(stockApplicationService.release(request)); }
    public StockOperationResultSnapshot deduct(StockOperationRequest request) { return toOperationSnapshot(stockApplicationService.deduct(request)); }

    private StockSnapshot toSnapshot(StockView source) {
        return new StockSnapshot(source.skuId(), null, source.totalStock(), source.lockedStock(), source.availableStock(), source.stockStatus(), source.lowStockThreshold(), source.highStockThreshold(), source.warningStatus(), source.source());
    }

    private StockOperationResultSnapshot toOperationSnapshot(StockOperationResult source) {
        return new StockOperationResultSnapshot(source.status(), source.businessType(), source.businessNo(), source.mode());
    }

    private StockOperationLogSnapshot toSnapshot(com.mallfei.stock.domain.model.StockOperationLog log) {
        return new StockOperationLogSnapshot(log.id(), log.skuId(), null, log.operationType(), log.businessType(), log.businessNo(), log.changeQuantity(), log.beforeTotalStock(), log.beforeLockedStock(), log.beforeAvailableStock(), log.afterTotalStock(), log.afterLockedStock(), log.afterAvailableStock(), log.remark(), log.operatorType(), log.operatorId(), log.operatorName(), log.sourceType(), log.createdAt());
    }
}
