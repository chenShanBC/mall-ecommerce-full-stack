package com.mallfei.order.application.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderTimeoutScheduleTask {

    private final OrderApplicationService orderApplicationService;

    public OrderTimeoutScheduleTask(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @Scheduled(fixedDelay = 60000)
    public void autoCancelTimedOutOrders() {
        orderApplicationService.autoCancelAllTimedOutOrders();
    }
}
