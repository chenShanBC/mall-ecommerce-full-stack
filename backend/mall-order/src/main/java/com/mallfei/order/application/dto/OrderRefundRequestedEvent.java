package com.mallfei.order.application.dto;

public record OrderRefundRequestedEvent(
        String orderNo,
        Long refundAmountCent,
        String reason,
        String refundNo
) {
}
