package com.mallfei.pay.domain.service;

public record PayRefundQueryResult(
        boolean success,
        boolean failed,
        String refundNo,
        String channelRefundNo,
        String tradeStatus,
        String message,
        String rawPayload
) {
    public static PayRefundQueryResult success(String refundNo, String channelRefundNo, String tradeStatus, String rawPayload) {
        return new PayRefundQueryResult(true, false, refundNo, channelRefundNo == null ? "" : channelRefundNo,
                tradeStatus == null ? "REFUND_SUCCESS" : tradeStatus, "", rawPayload == null ? "" : rawPayload);
    }

    public static PayRefundQueryResult failed(String refundNo, String tradeStatus, String message, String rawPayload) {
        return new PayRefundQueryResult(false, true, refundNo, "", tradeStatus == null ? "REFUND_FAILED" : tradeStatus,
                message == null ? "" : message, rawPayload == null ? "" : rawPayload);
    }

    public static PayRefundQueryResult unknown(String refundNo, String tradeStatus, String message, String rawPayload) {
        return new PayRefundQueryResult(false, false, refundNo, "", tradeStatus == null ? "UNKNOWN" : tradeStatus,
                message == null ? "" : message, rawPayload == null ? "" : rawPayload);
    }
}
