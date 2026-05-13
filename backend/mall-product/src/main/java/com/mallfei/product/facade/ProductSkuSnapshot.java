package com.mallfei.product.facade;

public record ProductSkuSnapshot(
        Long skuId,
        Long spuId,
        String spuName,
        String spuStatus,
        String mainImageUrl,
        String skuName,
        String skuCode,
        String specJson,
        Long salePriceCent,
        Long originPriceCent,
        String skuStatus
) {

    public boolean productOnline() {
        return "ONLINE".equalsIgnoreCase(spuStatus);
    }

    public boolean skuOnline() {
        return "ONLINE".equalsIgnoreCase(skuStatus);
    }
}
