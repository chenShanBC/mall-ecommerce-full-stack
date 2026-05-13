package com.mallfei.order.application.service;

import com.mallfei.order.application.dto.OrderTimeoutEvent;
import com.mallfei.order.config.OrderMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderTimeoutEventConsumer {

    private final OrderApplicationService orderApplicationService;

    public OrderTimeoutEventConsumer(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @RabbitListener(queues = OrderMqConfig.ORDER_TIMEOUT_PROCESS_QUEUE)
    public void consume(OrderTimeoutEvent event) {
        orderApplicationService.closeIfTimedOut(event.orderNo());
    }
}
