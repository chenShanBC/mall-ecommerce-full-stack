package com.mallfei.product.domain.model;

import com.mallfei.common.exception.BusinessException;

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
    public ProductSku {
        if (salePriceCent == null || salePriceCent <= 0) {
            throw BusinessException.badRequest("SKU售价必须大于0");
        }
        if (originPriceCent == null || originPriceCent <= 0) {
            throw BusinessException.badRequest("SKU原价必须大于0");
        }
        if (originPriceCent < salePriceCent) {
            throw BusinessException.badRequest("SKU原价必须大于等于售价");
        }
        if (!"ONLINE".equalsIgnoreCase(status) && !"OFFLINE".equalsIgnoreCase(status)) {
            throw BusinessException.badRequest("SKU状态仅支持 ONLINE 或 OFFLINE");
        }
    }

    public boolean enabled() {
        return "ONLINE".equalsIgnoreCase(status);
    }
}
