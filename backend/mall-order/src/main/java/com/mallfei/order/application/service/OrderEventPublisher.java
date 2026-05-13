package com.mallfei.order.application.service;

import com.mallfei.order.application.dto.OrderCancelledEvent;
import com.mallfei.order.config.OrderMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishCancelled(OrderCancelledEvent event) {
        rabbitTemplate.convertAndSend(OrderMqConfig.ORDER_EVENT_EXCHANGE, OrderMqConfig.ORDER_CANCELLED_ROUTING_KEY, event);
    }
}
