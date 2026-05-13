package com.mallfei.stock.domain.model;

import java.time.LocalDateTime;

public record StockOperationLog(
        Long id,
        Long skuId,
        String operationType,
        String businessType,
        String businessNo,
        Integer changeQuantity,
        Integer beforeTotalStock,
        Integer beforeLockedStock,
        Integer beforeAvailableStock,
        Integer afterTotalStock,
        Integer afterLockedStock,
        Integer afterAvailableStock,
        String remark,
        String operatorType,
        Long operatorId,
        String operatorName,
        String sourceType,
        LocalDateTime createdAt
) {

    public static StockOperationLog of(Long skuId,
                                       String operationType,
                                       String businessType,
                                       String businessNo,
                                       Integer changeQuantity,
                                       Stock before,
                                       Stock after,
                                       String remark,
                                       String operatorType,
                                       Long operatorId,
                                       String operatorName,
                                       String sourceType,
                                       LocalDateTime now) {
        return new StockOperationLog(
                null,
                skuId,
                operationType,
                businessType,
                businessNo,
                changeQuantity,
                before == null ? 0 : before.totalStock(),
                before == null ? 0 : before.lockedStock(),
                before == null ? 0 : before.availableStock(),
                after == null ? 0 : after.totalStock(),
                after == null ? 0 : after.lockedStock(),
                after == null ? 0 : after.availableStock(),
                remark == null ? "" : remark,
                operatorType == null ? "SYSTEM" : operatorType,
                operatorId,
                operatorName == null ? "system" : operatorName,
                sourceType == null ? "INTERNAL" : sourceType,
                now
        );
    }
}
