package com.mallfei.order.domain.model;

public record OrderItem(
        Long id,
        Long orderId,
        String orderNo,
        Long skuId,
        Long spuId,
        String skuName,
        String skuImageUrl,
        Long salePriceCent,
        Integer quantity,
        Long totalAmountCent
) {
}
