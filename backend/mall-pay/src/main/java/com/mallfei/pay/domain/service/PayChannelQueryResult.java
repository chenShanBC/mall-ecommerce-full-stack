package com.mallfei.pay.domain.service;

public record PayChannelQueryResult(
        boolean paid,
        String transactionNo,
        String tradeStatus,
        String rawPayload
) {
    public static PayChannelQueryResult unpaid(String tradeStatus, String rawPayload) {
        return new PayChannelQueryResult(false, "", tradeStatus, rawPayload);
    }

    public static PayChannelQueryResult paid(String transactionNo, String tradeStatus, String rawPayload) {
        return new PayChannelQueryResult(true, transactionNo == null ? "" : transactionNo, tradeStatus, rawPayload);
    }
}
