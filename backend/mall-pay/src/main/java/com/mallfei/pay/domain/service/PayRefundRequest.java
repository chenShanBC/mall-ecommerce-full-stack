package com.mallfei.pay.domain.service;

import com.mallfei.pay.domain.model.PayOrder;

public record PayRefundRequest(
        PayOrder payOrder,
        String refundNo,
        Long refundAmountCent,
        String reason
) {
}
