package com.mallfei.order.application.service;

import com.mallfei.order.application.dto.OrderCancelledEvent;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.domain.service.OrderDomainService;
import com.mallfei.stock.application.dto.StockOperationRequest;
import com.mallfei.stock.application.service.StockApplicationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderAdminOperationService {

    private final OrderDomainService orderDomainService;
    private final StockApplicationService stockApplicationService;
    private final OrderEventPublisher orderEventPublisher;

    public OrderAdminOperationService(OrderDomainService orderDomainService,
                                      StockApplicationService stockApplicationService,
                                      OrderEventPublisher orderEventPublisher) {
        this.orderDomainService = orderDomainService;
        this.stockApplicationService = stockApplicationService;
        this.orderEventPublisher = orderEventPublisher;
    }

    public void cancel(String orderNo) {
        Order order = orderDomainService.loadOrder(orderNo);
        Order updated = order.cancelByAdmin(LocalDateTime.now());
        if (updated.shouldReleaseStockAfterCancelled(order)) {
            stockApplicationService.cancel(new StockOperationRequest("ORDER", order.orderNo(), List.of()));
            orderEventPublisher.publishCancelled(new OrderCancelledEvent(order.orderNo(), Order.STATUS_CANCELLED));
        }
        orderDomainService.update(updated);
    }

    public void ship(String orderNo) {
        Order order = orderDomainService.loadOrder(orderNo);
        orderDomainService.update(order.ship(LocalDateTime.now()));
    }

    public void complete(String orderNo) {
        Order order = orderDomainService.loadOrder(orderNo);
        orderDomainService.update(order.complete(LocalDateTime.now()));
    }

    public void reviseReceiver(String orderNo, String receiverName, String receiverPhone, String receiverDetailAddress, String note) {
        Order order = orderDomainService.loadOrder(orderNo);
        Order updated = order.reviseReceiver(receiverName, receiverPhone, receiverDetailAddress).appendAdminRemark(note == null || note.isBlank() ? "ADMIN_REVISE_RECEIVER" : note);
        orderDomainService.update(updated);
    }

    public void markPaymentException(String orderNo, String note) {
        Order order = orderDomainService.loadOrder(orderNo);
        orderDomainService.update(order.appendAdminRemark(note == null || note.isBlank() ? "PAYMENT_EXCEPTION" : note));
    }
}
