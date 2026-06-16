package com.mallfei.pay.domain.service;

public record PayRefundResult(
        boolean success,
        String refundNo,
        String channelRefundNo,
        String code,
        String message,
        String rawPayload
) {
    public static PayRefundResult success(String refundNo, String channelRefundNo, String rawPayload) {
        return new PayRefundResult(true, refundNo, channelRefundNo == null ? "" : channelRefundNo, "SUCCESS", "", rawPayload == null ? "" : rawPayload);
    }

    public static PayRefundResult failed(String code, String message, String rawPayload) {
        return new PayRefundResult(false, "", "", code == null ? "FAILED" : code, message == null ? "" : message, rawPayload == null ? "" : rawPayload);
    }
}
