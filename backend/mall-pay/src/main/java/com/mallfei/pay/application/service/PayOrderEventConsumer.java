package com.mallfei.pay.application.service;

import com.mallfei.order.application.dto.OrderCancelledEvent;
import com.mallfei.order.config.OrderMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PayOrderEventConsumer {

    private final PayApplicationService payApplicationService;

    public PayOrderEventConsumer(PayApplicationService payApplicationService) {
        this.payApplicationService = payApplicationService;
    }

    @RabbitListener(queues = OrderMqConfig.ORDER_CANCELLED_QUEUE)
    public void onOrderCancelled(OrderCancelledEvent event) {
        payApplicationService.closePendingPayOrders(event.orderNo(), event.orderStatus());
    }
}
