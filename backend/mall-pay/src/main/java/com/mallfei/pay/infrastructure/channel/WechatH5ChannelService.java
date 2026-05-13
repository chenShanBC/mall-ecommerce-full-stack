package com.mallfei.pay.infrastructure.channel;

import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.pay.domain.service.PayChannelService;
import com.mallfei.pay.domain.service.PayChannelSubmitResult;
import org.springframework.stereotype.Component;

@Component
public class WechatH5ChannelService implements PayChannelService {

    @Override
    public String channelCode() {
        return PayOrder.CHANNEL_WECHAT_H5;
    }

    @Override
    public PayChannelSubmitResult submit(PayOrder payOrder) {
        String payload = "{\"channel\":\"WECHAT_H5\",\"status\":\"sandbox_not_connected\",\"payOrderNo\":\"" + payOrder.payOrderNo() + "\"}";
        return PayChannelSubmitResult.simple(channelCode(), payload);
    }
}
