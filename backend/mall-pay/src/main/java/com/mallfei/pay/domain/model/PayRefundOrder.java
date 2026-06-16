package com.mallfei.pay.domain.model;

import java.time.LocalDateTime;

public record PayRefundOrder(
        Long id,
        String refundNo,
        String orderNo,
        String payOrderNo,
        Long userId,
        String payChannel,
        Long refundAmountCent,
        String refundStatus,
        String transactionNo,
        String channelRefundNo,
        String requestPayload,
        String responsePayload,
        String failReason,
        LocalDateTime successAt,
        Integer version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static final String STATUS_PENDING = "REFUND_PENDING";
    public static final String STATUS_REFUNDING = "REFUNDING";
    public static final String STATUS_SUCCESS = "REFUND_SUCCESS";
    public static final String STATUS_FAILED = "REFUND_FAILED";

    public static PayRefundOrder create(PayOrder payOrder, String refundNo, Long refundAmountCent, String reason) {
        LocalDateTime now = LocalDateTime.now();
        return new PayRefundOrder(null, refundNo, payOrder.orderNo(), payOrder.payOrderNo(), payOrder.userId(), payOrder.payChannel(),
                refundAmountCent, STATUS_PENDING, payOrder.transactionNo(), "",
                "{\"reason\":\"" + safe(reason) + "\"}", "", "", null, 0, now, now);
    }

    public PayRefundOrder markRefunding() {
        return copy(STATUS_REFUNDING, channelRefundNo, responsePayload, "", null);
    }

    public PayRefundOrder markSuccess(String nextChannelRefundNo, String nextResponsePayload) {
        return copy(STATUS_SUCCESS, safe(nextChannelRefundNo), safe(nextResponsePayload), "", LocalDateTime.now());
    }

    public PayRefundOrder markFailed(String reason, String nextResponsePayload) {
        return copy(STATUS_FAILED, channelRefundNo, safe(nextResponsePayload), safe(reason), null);
    }

    private PayRefundOrder copy(String nextStatus, String nextChannelRefundNo, String nextResponsePayload, String nextFailReason, LocalDateTime nextSuccessAt) {
        return new PayRefundOrder(id, refundNo, orderNo, payOrderNo, userId, payChannel, refundAmountCent, nextStatus, transactionNo,
                nextChannelRefundNo, requestPayload, nextResponsePayload, nextFailReason, nextSuccessAt == null ? successAt : nextSuccessAt,
                version == null ? 0 : version + 1, createdAt, LocalDateTime.now());
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }
}
