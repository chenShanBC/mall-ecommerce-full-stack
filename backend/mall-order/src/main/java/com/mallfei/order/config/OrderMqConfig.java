package com.mallfei.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class OrderMqConfig {

    public static final String ORDER_TIMEOUT_DELAY_EXCHANGE = "order.timeout.delay.exchange";
    public static final String ORDER_TIMEOUT_DELAY_QUEUE = "order.timeout.delay.queue";
    public static final String ORDER_TIMEOUT_DELAY_ROUTING_KEY = "order.timeout.delay";

    public static final String ORDER_TIMEOUT_PROCESS_EXCHANGE = "order.timeout.process.exchange";
    public static final String ORDER_TIMEOUT_PROCESS_QUEUE = "order.timeout.process.queue";
    public static final String ORDER_TIMEOUT_PROCESS_ROUTING_KEY = "order.timeout.process";

    public static final String ORDER_EVENT_EXCHANGE = "order.event.exchange";
    public static final String ORDER_CANCELLED_QUEUE = "order.cancelled.queue";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";

    private final long orderTimeoutMinutes;

    public OrderMqConfig(@Value("${mall.order.timeout-minutes:2}") long orderTimeoutMinutes) {
        this.orderTimeoutMinutes = orderTimeoutMinutes;
    }

    @Bean
    public TopicExchange orderTimeoutDelayExchange() {
        return new TopicExchange(ORDER_TIMEOUT_DELAY_EXCHANGE);
    }

    @Bean
    public TopicExchange orderTimeoutProcessExchange() {
        return new TopicExchange(ORDER_TIMEOUT_PROCESS_EXCHANGE);
    }

    @Bean
    public TopicExchange orderEventExchange() {
        return new TopicExchange(ORDER_EVENT_EXCHANGE);
    }

    @Bean
    public Queue orderTimeoutDelayQueue() {
        return new Queue(ORDER_TIMEOUT_DELAY_QUEUE, true, false, false, Map.of(
                "x-message-ttl", orderTimeoutMinutes * 60 * 1000,
                "x-dead-letter-exchange", ORDER_TIMEOUT_PROCESS_EXCHANGE,
                "x-dead-letter-routing-key", ORDER_TIMEOUT_PROCESS_ROUTING_KEY
        ));
    }

    @Bean
    public Queue orderTimeoutProcessQueue() {
        return new Queue(ORDER_TIMEOUT_PROCESS_QUEUE, true);
    }

    @Bean
    public Queue orderCancelledQueue() {
        return new Queue(ORDER_CANCELLED_QUEUE, true);
    }

    @Bean
    public Binding orderTimeoutDelayBinding() {
        return BindingBuilder.bind(orderTimeoutDelayQueue()).to(orderTimeoutDelayExchange()).with(ORDER_TIMEOUT_DELAY_ROUTING_KEY);
    }

    @Bean
    public Binding orderTimeoutProcessBinding() {
        return BindingBuilder.bind(orderTimeoutProcessQueue()).to(orderTimeoutProcessExchange()).with(ORDER_TIMEOUT_PROCESS_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder.bind(orderCancelledQueue()).to(orderEventExchange()).with(ORDER_CANCELLED_ROUTING_KEY);
    }
}
