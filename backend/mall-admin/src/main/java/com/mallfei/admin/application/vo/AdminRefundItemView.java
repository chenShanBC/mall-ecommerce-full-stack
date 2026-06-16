package com.mallfei.admin.application.vo;

public record AdminRefundItemView(
        Long id,
        Long orderItemId,
        Long skuId,
        Integer quantity,
        Long refundAmountCent
) {
}
