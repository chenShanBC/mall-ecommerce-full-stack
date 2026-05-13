package com.mallfei.pay.infrastructure.channel;

import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.pay.domain.service.PayChannelCallbackRequest;
import com.mallfei.pay.domain.service.PayChannelClient;
import com.mallfei.pay.domain.service.PayChannelSubmitResult;
import org.springframework.stereotype.Component;

@Component
public class WechatPayClient implements PayChannelClient {

    @Override
    public String channelCode() {
        return PayOrder.CHANNEL_WECHAT_H5;
    }

    @Override
    public PayChannelSubmitResult submit(PayOrder payOrder) {
        String payload = "{\"channel\":\"WECHAT_H5\",\"status\":\"sandbox_ready_not_connected\",\"payOrderNo\":\"" + payOrder.payOrderNo() + "\",\"nextAction\":\"connect_wechat_sandbox\"}";
        return PayChannelSubmitResult.simple(channelCode(), payload);
    }

    @Override
    public boolean verifyCallback(PayChannelCallbackRequest request) {
        return request.signature() != null && !request.signature().isBlank();
    }
}
