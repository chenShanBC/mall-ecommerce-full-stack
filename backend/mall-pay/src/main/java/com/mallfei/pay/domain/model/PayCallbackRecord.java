package com.mallfei.pay.domain.model;

import java.time.LocalDateTime;

public record PayCallbackRecord(
        Long id,
        String channel,
        String callbackType,
        String payOrderNo,
        String refundNo,
        String orderNo,
        String outTradeNo,
        String transactionNo,
        Long amountCent,
        String tradeStatus,
        String signature,
        boolean verified,
        String processStatus,
        String failReason,
        String rawPayload,
        LocalDateTime callbackTime,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static final String TYPE_PAY = "PAY";
    public static final String TYPE_REFUND = "REFUND";

    public static final String STATUS_RECEIVED = "RECEIVED";
    public static final String STATUS_VERIFY_FAILED = "VERIFY_FAILED";
    public static final String STATUS_BUSINESS_MISMATCH = "BUSINESS_MISMATCH";
    public static final String STATUS_AMOUNT_MISMATCH = "AMOUNT_MISMATCH";
    public static final String STATUS_IGNORED_DUPLICATE = "IGNORED_DUPLICATE";
    public static final String STATUS_IGNORED_NON_SUCCESS = "IGNORED_NON_SUCCESS";
    public static final String STATUS_PROCESSED = "PROCESSED";
    public static final String STATUS_PROCESS_FAILED = "PROCESS_FAILED";

    public static PayCallbackRecord createPay(String channel,
                                              String payOrderNo,
                                              String orderNo,
                                              String outTradeNo,
                                              String transactionNo,
                                              Long amountCent,
                                              String tradeStatus,
                                              String signature,
                                              boolean verified,
                                              String rawPayload,
                                              LocalDateTime now) {
        return new PayCallbackRecord(null, channel, TYPE_PAY, payOrderNo, null, orderNo, outTradeNo, transactionNo,
                amountCent, tradeStatus, signature, verified, STATUS_RECEIVED, "", rawPayload, now, null, now, now);
    }

    public static PayCallbackRecord createRefund(String channel,
                                                 String payOrderNo,
                                                 String refundNo,
                                                 String orderNo,
                                                 String outTradeNo,
                                                 String transactionNo,
                                                 Long amountCent,
                                                 String tradeStatus,
                                                 String signature,
                                                 boolean verified,
                                                 String rawPayload,
                                                 LocalDateTime now) {
        return new PayCallbackRecord(null, channel, TYPE_REFUND, payOrderNo, refundNo, orderNo, outTradeNo, transactionNo,
                amountCent, tradeStatus, signature, verified, STATUS_RECEIVED, "", rawPayload, now, null, now, now);
    }

    public PayCallbackRecord markProcessed() {
        return copy(STATUS_PROCESSED, "", LocalDateTime.now());
    }

    public PayCallbackRecord markVerifyFailed(String reason) {
        return copy(STATUS_VERIFY_FAILED, reason, LocalDateTime.now());
    }

    public PayCallbackRecord markBusinessMismatch(String reason) {
        return copy(STATUS_BUSINESS_MISMATCH, reason, LocalDateTime.now());
    }

    public PayCallbackRecord markAmountMismatch(String reason) {
        return copy(STATUS_AMOUNT_MISMATCH, reason, LocalDateTime.now());
    }

    public PayCallbackRecord markIgnoredDuplicate(String reason) {
        return copy(STATUS_IGNORED_DUPLICATE, reason, LocalDateTime.now());
    }

    public PayCallbackRecord markIgnoredNonSuccess(String reason) {
        return copy(STATUS_IGNORED_NON_SUCCESS, reason, LocalDateTime.now());
    }

    public PayCallbackRecord markProcessFailed(String reason) {
        return copy(STATUS_PROCESS_FAILED, reason, LocalDateTime.now());
    }

    private PayCallbackRecord copy(String nextStatus, String nextFailReason, LocalDateTime now) {
        return new PayCallbackRecord(id, channel, callbackType, payOrderNo, refundNo, orderNo, outTradeNo, transactionNo,
                amountCent, tradeStatus, signature, verified, nextStatus, nextFailReason == null ? "" : nextFailReason,
                rawPayload, callbackTime, now, createdAt, now);
    }
}
