package com.mallfei.order.application.dto;

public record OrderRefundFailedEvent(
        String orderNo,
        String refundNo,
        Long refundAmountCent,
        String failReason
) {
}
