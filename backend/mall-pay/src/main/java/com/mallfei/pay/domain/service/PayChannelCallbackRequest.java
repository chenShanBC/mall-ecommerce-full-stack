package com.mallfei.pay.domain.service;

public record PayChannelCallbackRequest(
        String channelCode,
        String payOrderNo,
        String orderNo,
        String outTradeNo,
        String transactionNo,
        String signature,
        String rawPayload,
        String tradeStatus,
        Long amountCent
) {
    public PayChannelCallbackRequest(String channelCode,
                                     String payOrderNo,
                                     String orderNo,
                                     String outTradeNo,
                                     String transactionNo,
                                     String signature,
                                     String rawPayload,
                                     String tradeStatus) {
        this(channelCode, payOrderNo, orderNo, outTradeNo, transactionNo, signature, rawPayload, tradeStatus, null);
    }
}
