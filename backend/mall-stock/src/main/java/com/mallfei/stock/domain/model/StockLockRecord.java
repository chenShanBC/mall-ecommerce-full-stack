package com.mallfei.stock.domain.model;

import com.mallfei.common.exception.BusinessException;

import java.time.LocalDateTime;

public record StockLockRecord(
        Long id,
        String lockNo,
        Long skuId,
        String businessType,
        String businessNo,
        Integer quantity,
        String status,
        LocalDateTime lockTime,
        LocalDateTime releaseTime,
        LocalDateTime deductTime,
        Boolean reservedSynced,
        Boolean cancelledSynced,
        Boolean confirmedSynced
) {
    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_CONFIRMED = "CONFIRMED";

    public static StockLockRecord reserve(String lockNo, Long skuId, String businessType, String businessNo, Integer quantity, LocalDateTime now) {
        return new StockLockRecord(null, lockNo, skuId, businessType, businessNo, quantity, STATUS_RESERVED, now, null, null, false, false, false);
    }

    public boolean reserved() {
        return STATUS_RESERVED.equals(status);
    }

    public boolean reservedSyncedSafe() {
        return Boolean.TRUE.equals(reservedSynced);
    }

    public boolean cancelledSyncedSafe() {
        return Boolean.TRUE.equals(cancelledSynced);
    }

    public boolean confirmedSyncedSafe() {
        return Boolean.TRUE.equals(confirmedSynced);
    }

    public boolean syncedFor(String targetStatus) {
        if (STATUS_RESERVED.equals(targetStatus)) {
            return reservedSyncedSafe();
        }
        if (STATUS_CANCELLED.equals(targetStatus)) {
            return cancelledSyncedSafe();
        }
        if (STATUS_CONFIRMED.equals(targetStatus)) {
            return confirmedSyncedSafe();
        }
        throw BusinessException.badRequest("未知库存同步状态: " + targetStatus);
    }

    public StockLockRecord markSynced(String targetStatus) {
        if (STATUS_RESERVED.equals(targetStatus)) {
            return copy(status, releaseTime, deductTime, true, cancelledSyncedSafe(), confirmedSyncedSafe());
        }
        if (STATUS_CANCELLED.equals(targetStatus)) {
            return copy(status, releaseTime, deductTime, reservedSyncedSafe(), true, confirmedSyncedSafe());
        }
        if (STATUS_CONFIRMED.equals(targetStatus)) {
            return copy(status, releaseTime, deductTime, reservedSyncedSafe(), cancelledSyncedSafe(), true);
        }
        throw BusinessException.badRequest("未知库存同步状态: " + targetStatus);
    }

    public StockLockRecord cancel(LocalDateTime now) {
        if (STATUS_CANCELLED.equals(status)) {
            return this;
        }
        if (STATUS_CONFIRMED.equals(status)) {
            throw BusinessException.badRequest("库存已确认，不能取消: " + skuId);
        }
        if (!reserved()) {
            throw BusinessException.badRequest("库存取消失败: " + skuId);
        }
        return copy(STATUS_CANCELLED, now, deductTime, reservedSyncedSafe(), cancelledSyncedSafe(), confirmedSyncedSafe());
    }

    public StockLockRecord confirm(LocalDateTime now) {
        if (STATUS_CONFIRMED.equals(status)) {
            return this;
        }
        if (STATUS_CANCELLED.equals(status)) {
            throw BusinessException.badRequest("库存已取消，不能确认: " + skuId);
        }
        if (!reserved()) {
            throw BusinessException.badRequest("库存确认失败: " + skuId);
        }
        return copy(STATUS_CONFIRMED, releaseTime, now, reservedSyncedSafe(), cancelledSyncedSafe(), confirmedSyncedSafe());
    }

    public StockLockRecord syncTo(String targetStatus, LocalDateTime now) {
        if (STATUS_CANCELLED.equals(targetStatus)) {
            return cancel(now);
        }
        if (STATUS_CONFIRMED.equals(targetStatus)) {
            return confirm(now);
        }
        if (STATUS_RESERVED.equals(targetStatus)) {
            return this;
        }
        throw BusinessException.badRequest("未知库存锁状态: " + targetStatus);
    }

    private StockLockRecord copy(String targetStatus,
                                 LocalDateTime targetReleaseTime,
                                 LocalDateTime targetDeductTime,
                                 Boolean targetReservedSynced,
                                 Boolean targetCancelledSynced,
                                 Boolean targetConfirmedSynced) {
        return new StockLockRecord(id, lockNo, skuId, businessType, businessNo, quantity, targetStatus, lockTime, targetReleaseTime, targetDeductTime, targetReservedSynced, targetCancelledSynced, targetConfirmedSynced);
    }
}
