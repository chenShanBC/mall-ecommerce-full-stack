package com.mallfei.pay.domain.service;

import com.mallfei.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PayChannelClientRouter {

    private final Map<String, PayChannelClient> clientMap;

    public PayChannelClientRouter(List<PayChannelClient> clients) {
        this.clientMap = clients.stream().collect(Collectors.toMap(PayChannelClient::channelCode, Function.identity()));
    }

    public PayChannelClient route(String channelCode) {
        PayChannelClient client = clientMap.get(channelCode);
        if (client == null) {
            throw BusinessException.badRequest("暂不支持的支付渠道客户端: " + channelCode);
        }
        return client;
    }
}
