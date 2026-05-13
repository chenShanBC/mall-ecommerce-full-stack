package com.mallfei.cart.domain.model;

public record ProductSnapshot(
        Long skuId,
        Long spuId,
        String skuName,
        String skuImageUrl,
        Long salePriceCent
) {
}
