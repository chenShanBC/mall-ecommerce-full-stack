package com.mallfei.product.domain.model;

public record ProductSku(
        Long id,
        Long spuId,
        String skuCode,
        String skuName,
        String specJson,
        Long salePriceCent,
        Long originPriceCent,
        Integer salesCount,
        String status
) {
}
