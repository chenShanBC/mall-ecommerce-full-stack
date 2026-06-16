package com.mallfei.product.application.dto;

public record AdminProductPageQuery(
        String keyword,
        Long categoryId,
        String status,
        String salesBand,
        Integer hotSalesThreshold,
        Integer lowSalesThreshold,
        String sortBy,
        String sortOrder,
        long page,
        long size
) {
}
