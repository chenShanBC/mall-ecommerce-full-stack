package com.mallfei.pay.domain.service;

public record PayChannelSubmitResult(
        String channelCode,
        String displayPayload,
        String redirectForm,
        String redirectUrl
) {
    public static PayChannelSubmitResult simple(String channelCode, String displayPayload) {
        return new PayChannelSubmitResult(channelCode, displayPayload, "", "");
    }
}
