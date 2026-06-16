package com.mallfei.order.domain.model;

import com.mallfei.common.exception.BusinessException;

public record OrderRefundItem(
        Long id,
        Long refundId,
        String refundNo,
        Long orderItemId,
        Long skuId,
        Integer quantity,
        Long refundAmountCent
) {
    public static OrderRefundItem create(String refundNo, OrderItem orderItem, Integer quantity) {
        int safeQuantity = quantity == null ? 0 : quantity;
        if (safeQuantity <= 0) {
            throw BusinessException.badRequest("退款数量必须大于0");
        }
        if (orderItem.quantity() == null || safeQuantity > orderItem.quantity()) {
            throw BusinessException.badRequest("退款数量不能超过购买数量");
        }
        long unitPrice = orderItem.salePriceCent() == null ? 0L : orderItem.salePriceCent();
        return new OrderRefundItem(null, null, refundNo, orderItem.id(), orderItem.skuId(), safeQuantity, unitPrice * safeQuantity);
    }

    public OrderRefundItem bindRefundId(Long targetRefundId) {
        return new OrderRefundItem(id, targetRefundId, refundNo, orderItemId, skuId, quantity, refundAmountCent);
    }
}
