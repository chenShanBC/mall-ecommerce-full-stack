package com.mallfei.order.application.service;

import com.mallfei.order.application.dto.OrderTimeoutEvent;
import com.mallfei.order.config.OrderMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class OrderTimeoutEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public OrderTimeoutEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(OrderTimeoutEvent event) {
        publish(event, LocalDateTime.now().plusMinutes(2));
    }

    public void publish(OrderTimeoutEvent event, LocalDateTime expireTime) {
        long ttlMillis = Math.max(1_000L, Duration.between(LocalDateTime.now(), expireTime).toMillis());
        rabbitTemplate.convertAndSend(OrderMqConfig.ORDER_TIMEOUT_DELAY_EXCHANGE, OrderMqConfig.ORDER_TIMEOUT_DELAY_ROUTING_KEY, event, message -> {
            message.getMessageProperties().setExpiration(String.valueOf(ttlMillis));
            return message;
        });
    }
}
