package com.mallfei.order.application.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderAutoConfirmReceiptJob {

    private final OrderApplicationService orderApplicationService;

    public OrderAutoConfirmReceiptJob(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void autoConfirmShippedOrders() {
        orderApplicationService.autoConfirmShippedOrders();
    }
}
