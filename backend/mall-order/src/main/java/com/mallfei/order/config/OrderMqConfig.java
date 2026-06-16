package com.mallfei.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderMqConfig {

    public static final String ORDER_TIMEOUT_DELAY_EXCHANGE = "order.timeout.delay.exchange";
    public static final String ORDER_TIMEOUT_DELAY_QUEUE = "order.timeout.delay.queue";
    public static final String ORDER_TIMEOUT_DELAY_ROUTING_KEY = "order.timeout.delay";

    public static final String ORDER_TIMEOUT_PROCESS_EXCHANGE = "order.timeout.process.exchange";
    public static final String ORDER_TIMEOUT_PROCESS_QUEUE = "order.timeout.process.queue";
    public static final String ORDER_TIMEOUT_PROCESS_ROUTING_KEY = "order.timeout.process";
    public static final int ORDER_TIMEOUT_DELAY_QUEUE_TTL_MS = 900_000;

    public static final String ORDER_EVENT_EXCHANGE = "order.event.exchange";
    public static final String ORDER_CANCELLED_QUEUE = "order.cancelled.queue";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";
    public static final String ORDER_REFUND_REQUESTED_QUEUE = "order.refund.requested.queue";
    public static final String ORDER_REFUND_REQUESTED_ROUTING_KEY = "order.refund.requested";
    public static final String ORDER_REFUND_SUCCEEDED_QUEUE = "order.refund.succeeded.queue";
    public static final String ORDER_REFUND_SUCCEEDED_ROUTING_KEY = "order.refund.succeeded";
    public static final String ORDER_REFUND_FAILED_QUEUE = "order.refund.failed.queue";
    public static final String ORDER_REFUND_FAILED_ROUTING_KEY = "order.refund.failed";
    public static final String AFTERSALE_REFUND_SUCCEEDED_QUEUE = "aftersale.refund.succeeded.queue";
    public static final String AFTERSALE_REFUND_FAILED_QUEUE = "aftersale.refund.failed.queue";

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
        return buildOrderTimeoutDelayQueue();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(false);
        rabbitAdmin.setIgnoreDeclarationExceptions(true);
        return rabbitAdmin;
    }

    @Bean
    public ApplicationRunner orderRabbitCompatibilityRunner(RabbitAdmin rabbitAdmin) {
        return args -> {
            rabbitAdmin.deleteQueue(ORDER_TIMEOUT_DELAY_QUEUE);
            rabbitAdmin.declareExchange(orderTimeoutDelayExchange());
            rabbitAdmin.declareExchange(orderTimeoutProcessExchange());
            rabbitAdmin.declareExchange(orderEventExchange());
            rabbitAdmin.declareQueue(orderTimeoutDelayQueue());
            rabbitAdmin.declareQueue(orderTimeoutProcessQueue());
            rabbitAdmin.declareQueue(orderCancelledQueue());
            rabbitAdmin.declareQueue(orderRefundRequestedQueue());
            rabbitAdmin.declareQueue(orderRefundSucceededQueue());
            rabbitAdmin.declareQueue(orderRefundFailedQueue());
            rabbitAdmin.declareQueue(aftersaleRefundSucceededQueue());
            rabbitAdmin.declareQueue(aftersaleRefundFailedQueue());
            rabbitAdmin.declareBinding(orderTimeoutDelayBinding());
            rabbitAdmin.declareBinding(orderTimeoutProcessBinding());
            rabbitAdmin.declareBinding(orderCancelledBinding());
            rabbitAdmin.declareBinding(orderRefundRequestedBinding());
            rabbitAdmin.declareBinding(orderRefundSucceededBinding());
            rabbitAdmin.declareBinding(orderRefundFailedBinding());
            rabbitAdmin.declareBinding(aftersaleRefundSucceededBinding());
            rabbitAdmin.declareBinding(aftersaleRefundFailedBinding());
        };
    }

    private Queue buildOrderTimeoutDelayQueue() {
        return QueueBuilder.durable(ORDER_TIMEOUT_DELAY_QUEUE)
                .withArgument("x-message-ttl", ORDER_TIMEOUT_DELAY_QUEUE_TTL_MS)
                .withArgument("x-dead-letter-exchange", ORDER_TIMEOUT_PROCESS_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_TIMEOUT_PROCESS_ROUTING_KEY)
                .build();
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
    public Queue orderRefundRequestedQueue() {
        return new Queue(ORDER_REFUND_REQUESTED_QUEUE, true);
    }

    @Bean
    public Queue orderRefundSucceededQueue() {
        return new Queue(ORDER_REFUND_SUCCEEDED_QUEUE, true);
    }

    @Bean
    public Queue orderRefundFailedQueue() {
        return new Queue(ORDER_REFUND_FAILED_QUEUE, true);
    }

    @Bean
    public Queue aftersaleRefundSucceededQueue() {
        return new Queue(AFTERSALE_REFUND_SUCCEEDED_QUEUE, true);
    }

    @Bean
    public Queue aftersaleRefundFailedQueue() {
        return new Queue(AFTERSALE_REFUND_FAILED_QUEUE, true);
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

    @Bean
    public Binding orderRefundRequestedBinding() {
        return BindingBuilder.bind(orderRefundRequestedQueue()).to(orderEventExchange()).with(ORDER_REFUND_REQUESTED_ROUTING_KEY);
    }

    @Bean
    public Binding orderRefundSucceededBinding() {
        return BindingBuilder.bind(orderRefundSucceededQueue()).to(orderEventExchange()).with(ORDER_REFUND_SUCCEEDED_ROUTING_KEY);
    }

    @Bean
    public Binding orderRefundFailedBinding() {
        return BindingBuilder.bind(orderRefundFailedQueue()).to(orderEventExchange()).with(ORDER_REFUND_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding aftersaleRefundSucceededBinding() {
        return BindingBuilder.bind(aftersaleRefundSucceededQueue()).to(orderEventExchange()).with(ORDER_REFUND_SUCCEEDED_ROUTING_KEY);
    }

    @Bean
    public Binding aftersaleRefundFailedBinding() {
        return BindingBuilder.bind(aftersaleRefundFailedQueue()).to(orderEventExchange()).with(ORDER_REFUND_FAILED_ROUTING_KEY);
    }
}
