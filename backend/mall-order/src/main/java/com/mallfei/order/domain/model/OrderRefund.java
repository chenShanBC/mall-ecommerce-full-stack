package com.mallfei.order.domain.model;

import java.time.LocalDateTime;

public record OrderRefund(
        Long id,
        Long orderId,
        String orderNo,
        Long userId,
        String refundStatus,
        String refundReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static final String STATUS_PENDING = "REFUND_PENDING";
    public static final String STATUS_SUCCESS = "REFUND_SUCCESS";
    public static final String STATUS_FAILED = "REFUND_FAILED";
    public static final String STATUS_CLOSED = "REFUND_CLOSED";

    public static OrderRefund create(Long orderId, String orderNo, Long userId, String refundReason) {
        return new OrderRefund(null, orderId, orderNo, userId, STATUS_PENDING, refundReason, null, null);
    }

    public OrderRefund markSuccess() {
        return new OrderRefund(id, orderId, orderNo, userId, STATUS_SUCCESS, refundReason, createdAt, updatedAt);
    }
}
