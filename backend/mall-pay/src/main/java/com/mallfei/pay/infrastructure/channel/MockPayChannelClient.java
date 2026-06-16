package com.mallfei.pay.infrastructure.channel;

import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.pay.domain.service.PayChannelCallbackRequest;
import com.mallfei.pay.domain.service.PayChannelClient;
import com.mallfei.pay.domain.service.PayChannelQueryResult;
import com.mallfei.pay.domain.service.PayChannelSubmitResult;
import com.mallfei.pay.domain.service.PayRefundQueryRequest;
import com.mallfei.pay.domain.service.PayRefundQueryResult;
import com.mallfei.pay.domain.service.PayRefundRequest;
import com.mallfei.pay.domain.service.PayRefundResult;
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

    @Override
    public PayChannelQueryResult query(PayOrder payOrder) {
        if (payOrder.success() || mockChannelPaid(payOrder)) {
            String transactionNo = payOrder.transactionNo() == null || payOrder.transactionNo().isBlank()
                    ? "MOCK-" + payOrder.orderNo()
                    : payOrder.transactionNo();
            return PayChannelQueryResult.paid(transactionNo, "TRADE_SUCCESS", "{\"channel\":\"MOCK\",\"status\":\"TRADE_SUCCESS\"}");
        }
        return PayChannelQueryResult.unpaid(payOrder.payStatus(), "{\"channel\":\"MOCK\",\"status\":\"" + payOrder.payStatus() + "\"}");
    }

    private boolean mockChannelPaid(PayOrder payOrder) {
        String payload = payOrder.callbackPayload();
        return payload != null && payload.contains("\"mockChannelStatus\":\"SUCCESS\"");
    }

    @Override
    public PayRefundResult refund(PayRefundRequest request) {
        return PayRefundResult.success(request.refundNo(), "MOCK-REFUND-" + request.refundNo(), "{\"channel\":\"MOCK\",\"status\":\"REFUND_SUCCESS\"}");
    }

    @Override
    public PayRefundQueryResult queryRefund(PayRefundQueryRequest request) {
        return PayRefundQueryResult.success(request.refundNo(), "MOCK-REFUND-" + request.refundNo(), "REFUND_SUCCESS", "{\"channel\":\"MOCK\",\"status\":\"REFUND_SUCCESS\"}");
    }
}
