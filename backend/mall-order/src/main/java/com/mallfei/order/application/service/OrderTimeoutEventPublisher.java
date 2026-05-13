package com.mallfei.order.application.service;

import com.mallfei.order.application.dto.OrderTimeoutEvent;
import com.mallfei.order.config.OrderMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderTimeoutEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public OrderTimeoutEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(OrderTimeoutEvent event) {
        rabbitTemplate.convertAndSend(OrderMqConfig.ORDER_TIMEOUT_DELAY_EXCHANGE, OrderMqConfig.ORDER_TIMEOUT_DELAY_ROUTING_KEY, event);
    }
}
