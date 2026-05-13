package com.mallfei.order.domain.model;

public record ProductSnapshot(
        Long skuId,
        Long spuId,
        String skuName,
        String skuImageUrl,
        Long salePriceCent
) {
}
