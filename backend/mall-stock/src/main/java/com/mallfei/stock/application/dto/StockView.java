package com.mallfei.stock.application.dto;

public record StockView(
        Long skuId,
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
