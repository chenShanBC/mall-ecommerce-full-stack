package com.mallfei.pay.domain.service;

import com.mallfei.pay.domain.model.PayOrder;

public interface PayChannelClient {

    String channelCode();

    PayChannelSubmitResult submit(PayOrder payOrder);

    default PayChannelSubmitResult submit(PayOrder payOrder, String returnUrl) {
        return submit(payOrder);
    }

    default boolean verifyCallback(PayChannelCallbackRequest request) {
        return true;
    }
}
