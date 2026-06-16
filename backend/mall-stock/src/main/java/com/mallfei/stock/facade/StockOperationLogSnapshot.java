package com.mallfei.stock.facade;

import java.time.LocalDateTime;

public record StockOperationLogSnapshot(
        Long id,
        Long skuId,
        String skuName,
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
}
