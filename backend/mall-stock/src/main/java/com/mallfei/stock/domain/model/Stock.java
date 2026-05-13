package com.mallfei.stock.domain.model;

import com.mallfei.common.exception.BusinessException;

public record Stock(
        Long id,
        Long skuId,
        Integer totalStock,
        Integer lockedStock,
        Integer availableStock,
        String stockStatus,
        Integer lowStockThreshold,
        Integer highStockThreshold,
        String warningStatus,
        Integer version
) {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_FROZEN = "FROZEN";
    public static final String STATUS_OFFLINE = "OFFLINE";

    public static final String WARNING_NORMAL = "NORMAL";
    public static final String WARNING_LOW = "LOW";
    public static final String WARNING_HIGH = "HIGH";

    public static Stock initialize(Long skuId, Integer initialStock) {
        int safeStock = Math.max(initialStock == null ? 0 : initialStock, 0);
        int lowThreshold = Math.min(10, safeStock);
        int highThreshold = Math.max(1000, safeStock);
        return new Stock(null, skuId, safeStock, 0, safeStock, STATUS_ACTIVE, lowThreshold, highThreshold,
                calculateWarningStatus(safeStock, lowThreshold, highThreshold), 0);
    }

    public boolean active() {
        return STATUS_ACTIVE.equals(stockStatus);
    }

    public void ensureSellable() {
        if (STATUS_OFFLINE.equals(stockStatus)) {
            throw BusinessException.badRequest("SKU库存状态为下架，无法销售: " + skuId);
        }
        if (STATUS_FROZEN.equals(stockStatus)) {
            throw BusinessException.badRequest("SKU库存已冻结，无法销售: " + skuId);
        }
    }

    public void ensureAvailable(Integer quantity) {
        ensureSellable();
        if (availableStock == null || quantity == null || availableStock < quantity) {
            throw BusinessException.badRequest("SKU库存不足: " + skuId);
        }
    }

    public Stock reserve(Integer quantity) {
        int reserveQuantity = quantity == null ? 0 : quantity;
        ensureAvailable(reserveQuantity);
        int currentAvailable = availableStock == null ? 0 : availableStock;
        int currentLocked = lockedStock == null ? 0 : lockedStock;
        int nextAvailable = currentAvailable - reserveQuantity;
        return copy(totalStock, currentLocked + reserveQuantity, nextAvailable, stockStatus, lowStockThreshold, highStockThreshold);
    }

    public Stock release(Integer quantity) {
        int releaseQuantity = quantity == null ? 0 : quantity;
        int currentAvailable = availableStock == null ? 0 : availableStock;
        int currentLocked = lockedStock == null ? 0 : lockedStock;
        if (currentLocked < releaseQuantity) {
            throw BusinessException.badRequest("锁定库存不足，无法回补: " + skuId);
        }
        int nextAvailable = currentAvailable + releaseQuantity;
        return copy(totalStock, currentLocked - releaseQuantity, nextAvailable, stockStatus, lowStockThreshold, highStockThreshold);
    }

    public Stock confirm(Integer quantity) {
        int confirmQuantity = quantity == null ? 0 : quantity;
        int currentTotal = totalStock == null ? 0 : totalStock;
        int currentLocked = lockedStock == null ? 0 : lockedStock;
        int currentAvailable = availableStock == null ? 0 : availableStock;
        if (currentLocked < confirmQuantity || currentTotal < confirmQuantity) {
            throw BusinessException.badRequest("库存锁定数量不足，无法扣减: " + skuId);
        }
        return copy(currentTotal - confirmQuantity, currentLocked - confirmQuantity, currentAvailable, stockStatus, lowStockThreshold, highStockThreshold);
    }

    public Stock restore(Integer quantity) {
        int restoreQuantity = quantity == null ? 0 : quantity;
        int currentAvailable = availableStock == null ? 0 : availableStock;
        int currentTotal = totalStock == null ? 0 : totalStock;
        int nextAvailable = currentAvailable + restoreQuantity;
        return copy(currentTotal + restoreQuantity, lockedStock, nextAvailable, stockStatus, lowStockThreshold, highStockThreshold);
    }

    public Stock applyPolicy(String stockStatus, Integer lowStockThreshold, Integer highStockThreshold) {
        String nextStatus = normalizeStockStatus(stockStatus == null || stockStatus.isBlank() ? this.stockStatus : stockStatus);
        int nextLow = lowStockThreshold == null ? safeLowThreshold() : Math.max(0, lowStockThreshold);
        int nextHigh = highStockThreshold == null ? safeHighThreshold() : Math.max(nextLow, highStockThreshold);
        return copy(totalStock, lockedStock, availableStock, nextStatus, nextLow, nextHigh);
    }

    public Stock touchVersion() {
        return new Stock(id, skuId, totalStock, lockedStock, availableStock, normalizeStockStatus(stockStatus), safeLowThreshold(), safeHighThreshold(), safeWarningStatus(), nextVersion());
    }

    private Stock copy(Integer totalStock,
                       Integer lockedStock,
                       Integer availableStock,
                       String stockStatus,
                       Integer lowStockThreshold,
                       Integer highStockThreshold) {
        String normalizedStatus = normalizeStockStatus(stockStatus);
        int normalizedLow = Math.max(0, lowStockThreshold == null ? 0 : lowStockThreshold);
        int normalizedHigh = Math.max(normalizedLow, highStockThreshold == null ? normalizedLow : highStockThreshold);
        int normalizedAvailable = availableStock == null ? 0 : availableStock;
        return new Stock(
                id,
                skuId,
                totalStock == null ? 0 : totalStock,
                lockedStock == null ? 0 : lockedStock,
                normalizedAvailable,
                normalizedStatus,
                normalizedLow,
                normalizedHigh,
                calculateWarningStatus(normalizedAvailable, normalizedLow, normalizedHigh),
                nextVersion()
        );
    }

    private static String calculateWarningStatus(int availableStock, int lowStockThreshold, int highStockThreshold) {
        if (availableStock <= lowStockThreshold) {
            return WARNING_LOW;
        }
        if (availableStock >= highStockThreshold) {
            return WARNING_HIGH;
        }
        return WARNING_NORMAL;
    }

    private String normalizeStockStatus(String stockStatus) {
        if (STATUS_FROZEN.equals(stockStatus) || STATUS_OFFLINE.equals(stockStatus)) {
            return stockStatus;
        }
        return STATUS_ACTIVE;
    }

    private int safeLowThreshold() {
        return Math.max(0, lowStockThreshold == null ? 0 : lowStockThreshold);
    }

    private int safeHighThreshold() {
        return Math.max(safeLowThreshold(), highStockThreshold == null ? safeLowThreshold() : highStockThreshold);
    }

    private String safeWarningStatus() {
        return warningStatus == null || warningStatus.isBlank()
                ? calculateWarningStatus(availableStock == null ? 0 : availableStock, safeLowThreshold(), safeHighThreshold())
                : warningStatus;
    }

    private Integer nextVersion() {
        return version == null ? 0 : version + 1;
    }
}
