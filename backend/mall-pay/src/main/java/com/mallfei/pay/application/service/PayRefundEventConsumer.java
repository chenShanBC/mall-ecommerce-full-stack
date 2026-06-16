package com.mallfei.pay.application.service;

import com.mallfei.order.application.dto.OrderRefundRequestedEvent;
import com.mallfei.order.config.OrderMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PayRefundEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PayRefundEventConsumer.class);

    private final PayApplicationService payApplicationService;

    public PayRefundEventConsumer(PayApplicationService payApplicationService) {
        this.payApplicationService = payApplicationService;
    }

    @RabbitListener(queues = OrderMqConfig.ORDER_REFUND_REQUESTED_QUEUE)
    public void onRefundRequested(OrderRefundRequestedEvent event) {
        log.info("Received order refund requested event, orderNo={}, refundNo={}, amountCent={}",
                event.orderNo(), event.refundNo(), event.refundAmountCent());
        try {
            payApplicationService.refund(event.orderNo(), event.refundAmountCent(), event.reason(), event.refundNo());
        } catch (Exception exception) {
            log.error("Failed to process order refund requested event, orderNo={}, refundNo={}, message={}",
                    event.orderNo(), event.refundNo(), exception.getMessage(), exception);
        }
    }
}
