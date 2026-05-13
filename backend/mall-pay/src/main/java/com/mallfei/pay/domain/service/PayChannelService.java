package com.mallfei.pay.domain.service;

import com.mallfei.pay.domain.model.PayOrder;

public interface PayChannelService {

    String channelCode();

    PayChannelSubmitResult submit(PayOrder payOrder);
}
