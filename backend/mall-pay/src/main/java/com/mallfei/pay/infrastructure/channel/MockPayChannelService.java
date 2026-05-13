package com.mallfei.pay.infrastructure.channel;

import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.pay.domain.service.PayChannelService;
import com.mallfei.pay.domain.service.PayChannelSubmitResult;
import org.springframework.stereotype.Component;

@Component
public class MockPayChannelService implements PayChannelService {

    @Override
    public String channelCode() {
        return PayOrder.CHANNEL_MOCK;
    }

    @Override
    public PayChannelSubmitResult submit(PayOrder payOrder) {
        String payload = "{\"channel\":\"MOCK\",\"payOrderNo\":\"" + payOrder.payOrderNo() + "\",\"hint\":\"mock submit success\"}";
        return PayChannelSubmitResult.simple(channelCode(), payload);
    }
}
