package com.mallfei.pay.domain.service;

import com.mallfei.pay.domain.model.PayOrder;

public record PayRefundQueryRequest(
        PayOrder payOrder,
        String refundNo
) {
}
