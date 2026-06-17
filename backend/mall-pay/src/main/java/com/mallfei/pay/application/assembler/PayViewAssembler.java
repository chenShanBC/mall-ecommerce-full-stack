package com.mallfei.pay.application.assembler;

import com.mallfei.order.domain.model.Order;
import com.mallfei.pay.application.vo.PayOrderView;
import com.mallfei.pay.application.vo.PayReconcileResultView;
import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.pay.domain.service.PayChannelSubmitResult;
import org.springframework.stereotype.Component;

@Component
public class PayViewAssembler {

    public PayOrderView toView(PayOrder payOrder) {
        return new PayOrderView(
                payOrder.payOrderNo(),
                payOrder.orderNo(),
                payOrder.payStatus(),
                payOrder.payAmountCent(),
                payOrder.payChannel(),
                payOrder.transactionNo(),
                payOrder.idempotentKey(),
                payOrder.createdAt() == null ? "" : payOrder.createdAt().toString(),
                payOrder.callbackPayload(),
                "",
                ""
        );
    }

    public PayOrderView toView(PayOrder payOrder, PayChannelSubmitResult submitResult) {
        return new PayOrderView(
                payOrder.payOrderNo(),
                payOrder.orderNo(),
                payOrder.payStatus(),
                payOrder.payAmountCent(),
                payOrder.payChannel(),
                payOrder.transactionNo(),
                payOrder.idempotentKey(),
                payOrder.createdAt() == null ? "" : payOrder.createdAt().toString(),
                payOrder.callbackPayload(),
                submitResult == null ? "" : submitResult.redirectForm(),
                submitResult == null ? "" : submitResult.redirectUrl()
        );
    }

    public PayReconcileResultView toReconcileResult(String orderNo, Order order, PayOrder payOrder, boolean consistent) {
        if (payOrder == null) {
            return new PayReconcileResultView(
                    orderNo,
                    order.orderStatus(),
                    order.payAmountCent(),
                    false,
                    null,
                    null,
                    null,
                    true
            );
        }
        return new PayReconcileResultView(
                orderNo,
                order.orderStatus(),
                order.payAmountCent(),
                true,
                payOrder.payOrderNo(),
                payOrder.payStatus(),
                payOrder.payAmountCent(),
                consistent
        );
    }
}
