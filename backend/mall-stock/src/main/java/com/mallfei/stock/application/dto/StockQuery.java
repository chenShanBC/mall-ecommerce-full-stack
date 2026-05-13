package com.mallfei.stock.application.dto;

public record StockQuery(
        Long skuId,
        String stockStatus,
        String warningStatus,
        String sortBy,
        String sortOrder,
        long page,
        long size
) {
}
