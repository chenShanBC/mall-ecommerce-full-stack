package com.mallfei.order.application.dto;

public record OrderRefundSucceededEvent(
        String orderNo,
        String refundNo,
        Long refundAmountCent,
        String channelRefundNo
) {
}
