package com.mallfei.pay.infrastructure.channel;

import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.pay.domain.service.PayChannelCallbackRequest;
import com.mallfei.pay.domain.service.PayChannelClient;
import com.mallfei.pay.domain.service.PayChannelSubmitResult;
import org.springframework.stereotype.Component;

@Component
public class MockPayChannelClient implements PayChannelClient {

    @Override
    public String channelCode() {
        return PayOrder.CHANNEL_MOCK;
    }

    @Override
    public PayChannelSubmitResult submit(PayOrder payOrder) {
        String payload = "{\"channel\":\"MOCK\",\"payOrderNo\":\"" + payOrder.payOrderNo() + "\",\"hint\":\"mock submit success\"}";
        return PayChannelSubmitResult.simple(channelCode(), payload);
    }

    @Override
    public boolean verifyCallback(PayChannelCallbackRequest request) {
        return true;
    }
}
