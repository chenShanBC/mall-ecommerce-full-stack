package com.mallfei.stock.application.service;

import com.mallfei.common.exception.BusinessException;
import com.mallfei.stock.domain.model.Stock;
import com.mallfei.stock.domain.model.StockLockRecord;
import com.mallfei.stock.domain.repository.StockLockRepository;
import com.mallfei.stock.domain.repository.StockRepository;
import com.mallfei.stock.domain.service.StockPersistenceDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class StockPersistenceService {

    private final StockRepository stockRepository;
    private final StockLockRepository stockLockRepository;
    private final StockPersistenceDomainService stockPersistenceDomainService;
    private final StockOperationLogApplicationService stockOperationLogApplicationService;

    public StockPersistenceService(StockRepository stockRepository,
                                   StockLockRepository stockLockRepository,
                                   StockPersistenceDomainService stockPersistenceDomainService,
                                   StockOperationLogApplicationService stockOperationLogApplicationService) {
        this.stockRepository = stockRepository;
        this.stockLockRepository = stockLockRepository;
        this.stockPersistenceDomainService = stockPersistenceDomainService;
        this.stockOperationLogApplicationService = stockOperationLogApplicationService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncReservation(String businessType, String businessNo, Long skuId, String targetStatus) {
        Stock before = stockRepository.findBySkuId(skuId)
                .orElseThrow(() -> BusinessException.badRequest("库存不存在: " + skuId));
        StockLockRecord record = stockLockRepository.findByBusiness(businessType, businessNo, skuId)
                .orElseThrow(() -> BusinessException.badRequest("库存锁记录不存在: " + skuId));
        if (record.syncedFor(targetStatus)) {
            return;
        }
        applyAtomicStockSync(record, targetStatus);
        StockLockRecord syncedRecord = stockPersistenceDomainService.transitionAndMarkSynced(record, targetStatus, LocalDateTime.now());
        stockLockRepository.update(syncedRecord);
        Stock after = stockRepository.findBySkuId(skuId)
                .orElseThrow(() -> BusinessException.badRequest("库存不存在: " + skuId));
        stockOperationLogApplicationService.recordIfAbsent(
                skuId,
                operationType(targetStatus),
                businessType,
                businessNo,
                changeQuantity(record, targetStatus),
                before,
                after,
                syncRemark(targetStatus),
                "MQ_STOCK_SYNC"
        );
    }

    private void applyAtomicStockSync(StockLockRecord record, String targetStatus) {
        if (StockLockRecord.STATUS_RESERVED.equals(targetStatus)) {
            stockRepository.applyReservedSync(record.skuId(), record.quantity());
            return;
        }
        if (StockLockRecord.STATUS_CANCELLED.equals(targetStatus)) {
            ensureReservedSyncedBeforeTerminalSync(record);
            stockRepository.applyCancelledSync(record.skuId(), record.quantity());
            return;
        }
        if (StockLockRecord.STATUS_CONFIRMED.equals(targetStatus)) {
            ensureReservedSyncedBeforeTerminalSync(record);
            stockRepository.applyConfirmedSync(record.skuId(), record.quantity());
            return;
        }
        throw BusinessException.badRequest("未知库存同步目标状态: " + targetStatus);
    }

    private void ensureReservedSyncedBeforeTerminalSync(StockLockRecord record) {
        if (record.reservedSyncedSafe()) {
            return;
        }
        stockRepository.applyReservedSync(record.skuId(), record.quantity());
        stockLockRepository.update(stockPersistenceDomainService.markReservedSynced(record));
    }

    private String operationType(String targetStatus) {
        if (StockLockRecord.STATUS_RESERVED.equals(targetStatus)) return "RESERVE";
        if (StockLockRecord.STATUS_CANCELLED.equals(targetStatus)) return "CANCEL";
        if (StockLockRecord.STATUS_CONFIRMED.equals(targetStatus)) return "CONFIRM";
        throw BusinessException.badRequest("未知库存同步目标状态: " + targetStatus);
    }

    private Integer changeQuantity(StockLockRecord record, String targetStatus) {
        if (StockLockRecord.STATUS_CANCELLED.equals(targetStatus)) return record.quantity();
        return -record.quantity();
    }

    private String syncRemark(String targetStatus) {
        if (StockLockRecord.STATUS_RESERVED.equals(targetStatus)) return "MQ预占库存落库";
        if (StockLockRecord.STATUS_CANCELLED.equals(targetStatus)) return "MQ取消预占落库";
        if (StockLockRecord.STATUS_CONFIRMED.equals(targetStatus)) return "MQ确认扣减落库";
        throw BusinessException.badRequest("未知库存同步目标状态: " + targetStatus);
    }
}
