package com.mallfei.stock.domain.service;

import com.mallfei.common.exception.BusinessException;
import com.mallfei.stock.domain.model.Stock;
import com.mallfei.stock.domain.model.StockLockRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class StockPersistenceDomainService {

    public StockPersistencePlan plan(Stock stock, StockLockRecord record, String targetStatus, LocalDateTime now) {
        if (record.syncedFor(targetStatus)) {
            return StockPersistencePlan.noop(stock, record, targetStatus);
        }
        if (StockLockRecord.STATUS_RESERVED.equals(targetStatus)) {
            Stock updatedStock = stock.reserve(record.quantity());
            StockLockRecord updatedRecord = record.markSynced(targetStatus);
            return StockPersistencePlan.changed(updatedStock, updatedRecord, targetStatus, "RESERVE", -record.quantity(), "MQ预占库存落库");
        }
        if (StockLockRecord.STATUS_CANCELLED.equals(targetStatus)) {
            StockLockRecord transitionedRecord = record.syncTo(targetStatus, now);
            Stock updatedStock = stock.release(record.quantity());
            StockLockRecord updatedRecord = transitionedRecord.markSynced(targetStatus);
            return StockPersistencePlan.changed(updatedStock, updatedRecord, targetStatus, "CANCEL", record.quantity(), "MQ取消预占落库");
        }
        if (StockLockRecord.STATUS_CONFIRMED.equals(targetStatus)) {
            StockLockRecord transitionedRecord = record.syncTo(targetStatus, now);
            Stock updatedStock = stock.confirmConvergent(record.quantity());
            StockLockRecord updatedRecord = transitionedRecord.markSynced(targetStatus);
            return StockPersistencePlan.changed(updatedStock, updatedRecord, targetStatus, "CONFIRM", -record.quantity(), "MQ确认扣减落库");
        }
        throw BusinessException.badRequest("未知库存同步目标状态: " + targetStatus);
    }

    public StockLockRecord markReservedSynced(StockLockRecord record) {
        return record.markSynced(StockLockRecord.STATUS_RESERVED);
    }

    public StockLockRecord transitionAndMarkSynced(StockLockRecord record, String targetStatus, LocalDateTime now) {
        if (StockLockRecord.STATUS_RESERVED.equals(targetStatus)) {
            return markReservedSynced(record);
        }
        return record.syncTo(targetStatus, now).markSynced(targetStatus);
    }

    public RedisRecoveryPlan planRedisRecovery(Stock stock, long unsyncedReservedQuantity) {
        int safeUnsyncedReserved = Math.toIntExact(Math.max(0L, unsyncedReservedQuantity));
        int dbTotal = stock.totalStock() == null ? 0 : stock.totalStock();
        int dbLocked = stock.lockedStock() == null ? 0 : stock.lockedStock();
        int dbAvailable = stock.availableStock() == null ? 0 : stock.availableStock();
        int recoveryLocked = dbLocked + safeUnsyncedReserved;
        int recoveryAvailable = dbAvailable - safeUnsyncedReserved;
        if (recoveryAvailable < 0 || recoveryLocked > dbTotal) {
            throw BusinessException.badRequest("库存恢复计划异常，DB库存与未同步预占不一致: sku=" + stock.skuId());
        }
        return new RedisRecoveryPlan(stock.skuId(), dbTotal, recoveryLocked, recoveryAvailable, safeUnsyncedReserved);
    }

    public record StockPersistencePlan(
            Stock stock,
            StockLockRecord record,
            String targetStatus,
            String operationType,
            Integer changeQuantity,
            String remark,
            boolean changed
    ) {
        public static StockPersistencePlan noop(Stock stock, StockLockRecord record, String targetStatus) {
            return new StockPersistencePlan(stock, record, targetStatus, null, 0, "库存同步幂等跳过", false);
        }

        public static StockPersistencePlan changed(Stock stock,
                                                   StockLockRecord record,
                                                   String targetStatus,
                                                   String operationType,
                                                   Integer changeQuantity,
                                                   String remark) {
            return new StockPersistencePlan(stock, record, targetStatus, operationType, changeQuantity, remark, true);
        }
    }

    public record RedisRecoveryPlan(
            Long skuId,
            Integer totalStock,
            Integer lockedStock,
            Integer availableStock,
            Integer unsyncedReservedQuantity
    ) {
    }
}
