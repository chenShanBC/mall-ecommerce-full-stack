package com.mallfei.pay.domain.model;

import java.time.LocalDateTime;

public record PayCallbackRecord(
        Long id,
        String channel,
        String payOrderNo,
        String orderNo,
        String outTradeNo,
        String transactionNo,
        String signature,
        boolean verified,
        String processStatus,
        String rawPayload,
        LocalDateTime callbackTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static final String STATUS_INIT = "INIT";
    public static final String STATUS_PROCESSED = "PROCESSED";
    public static final String STATUS_IGNORED = "IGNORED";
    public static final String STATUS_FAILED = "FAILED";

    public static PayCallbackRecord create(String channel,
                                           String payOrderNo,
                                           String orderNo,
                                           String outTradeNo,
                                           String transactionNo,
                                           String signature,
                                           boolean verified,
                                           String rawPayload,
                                           LocalDateTime now) {
        return new PayCallbackRecord(null, channel, payOrderNo, orderNo, outTradeNo, transactionNo, signature, verified, STATUS_INIT, rawPayload, now, now, now);
    }

    public PayCallbackRecord markProcessed() {
        return new PayCallbackRecord(id, channel, payOrderNo, orderNo, outTradeNo, transactionNo, signature, verified, STATUS_PROCESSED, rawPayload, callbackTime, createdAt, LocalDateTime.now());
    }

    public PayCallbackRecord markIgnored() {
        return new PayCallbackRecord(id, channel, payOrderNo, orderNo, outTradeNo, transactionNo, signature, verified, STATUS_IGNORED, rawPayload, callbackTime, createdAt, LocalDateTime.now());
    }

    public PayCallbackRecord markFailed() {
        return new PayCallbackRecord(id, channel, payOrderNo, orderNo, outTradeNo, transactionNo, signature, verified, STATUS_FAILED, rawPayload, callbackTime, createdAt, LocalDateTime.now());
    }
}
