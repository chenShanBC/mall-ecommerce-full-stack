package com.mallfei.stock.facade;

public record StockSnapshot(
        Long skuId,
        String skuName,
        Integer totalStock,
        Integer lockedStock,
        Integer availableStock,
        String stockStatus,
        Integer lowStockThreshold,
        Integer highStockThreshold,
        String warningStatus,
        String source
) {
}
