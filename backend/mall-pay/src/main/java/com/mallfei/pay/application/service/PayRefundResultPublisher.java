package com.mallfei.pay.application.service;

import com.mallfei.order.application.dto.OrderRefundFailedEvent;
import com.mallfei.order.application.dto.OrderRefundSucceededEvent;
import com.mallfei.order.config.OrderMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PayRefundResultPublisher {

    private final RabbitTemplate rabbitTemplate;

    public PayRefundResultPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishSucceeded(String orderNo, String refundNo, Long refundAmountCent, String channelRefundNo) {
        rabbitTemplate.convertAndSend(OrderMqConfig.ORDER_EVENT_EXCHANGE,
                OrderMqConfig.ORDER_REFUND_SUCCEEDED_ROUTING_KEY,
                new OrderRefundSucceededEvent(orderNo, refundNo, refundAmountCent, channelRefundNo));
    }

    public void publishFailed(String orderNo, String refundNo, Long refundAmountCent, String failReason) {
        rabbitTemplate.convertAndSend(OrderMqConfig.ORDER_EVENT_EXCHANGE,
                OrderMqConfig.ORDER_REFUND_FAILED_ROUTING_KEY,
                new OrderRefundFailedEvent(orderNo, refundNo, refundAmountCent, failReason));
    }
}
