package com.mallfei.pay.domain.service;

import com.mallfei.pay.domain.model.PayOrder;

import java.time.Duration;

public interface PayChannelClient {

    String channelCode();

    PayChannelSubmitResult submit(PayOrder payOrder);

    default PayChannelSubmitResult submit(PayOrder payOrder, String returnUrl) {
        return submit(payOrder);
    }

    default PayChannelSubmitResult submit(PayOrder payOrder, String returnUrl, Duration remainingPayTime) {
        return submit(payOrder, returnUrl);
    }

    default boolean verifyCallback(PayChannelCallbackRequest request) {
        return true;
    }

    default PayChannelQueryResult query(PayOrder payOrder) {
        return PayChannelQueryResult.unpaid(payOrder.payStatus(), "");
    }

    default PayRefundResult refund(PayRefundRequest request) {
        return PayRefundResult.failed("UNSUPPORTED", "当前支付渠道暂不支持退款", "");
    }

    default PayRefundQueryResult queryRefund(PayRefundQueryRequest request) {
        return PayRefundQueryResult.unknown(request.refundNo(), "UNKNOWN", "当前支付渠道暂不支持退款查询", "");
    }
}
