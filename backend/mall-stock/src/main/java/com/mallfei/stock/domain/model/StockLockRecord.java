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
        LocalDateTime deductTime
) {
    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_CONFIRMED = "CONFIRMED";

    public static StockLockRecord reserve(String lockNo, Long skuId, String businessType, String businessNo, Integer quantity, LocalDateTime now) {
        return new StockLockRecord(null, lockNo, skuId, businessType, businessNo, quantity, STATUS_RESERVED, now, null, null);
    }

    public boolean reserved() {
        return STATUS_RESERVED.equals(status);
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
        return copy(STATUS_CANCELLED, now, deductTime);
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
        return copy(STATUS_CONFIRMED, releaseTime, now);
    }

    public StockLockRecord syncTo(String targetStatus, LocalDateTime now) {
        if (targetStatus.equals(status)) {
            return this;
        }
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

    private StockLockRecord copy(String targetStatus, LocalDateTime targetReleaseTime, LocalDateTime targetDeductTime) {
        return new StockLockRecord(id, lockNo, skuId, businessType, businessNo, quantity, targetStatus, lockTime, targetReleaseTime, targetDeductTime);
    }
}
