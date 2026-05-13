package com.mallfei.pay.domain.service;

import com.mallfei.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PayChannelRouter {

    private final Map<String, PayChannelService> serviceMap;

    public PayChannelRouter(List<PayChannelService> services) {
        this.serviceMap = services.stream().collect(Collectors.toMap(PayChannelService::channelCode, Function.identity()));
    }

    public PayChannelService route(String channelCode) {
        PayChannelService service = serviceMap.get(channelCode);
        if (service == null) {
            throw BusinessException.badRequest("暂不支持的支付渠道: " + channelCode);
        }
        return service;
    }
}
