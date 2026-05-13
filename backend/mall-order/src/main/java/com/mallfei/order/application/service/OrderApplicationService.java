package com.mallfei.order.application.service;

import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.application.assembler.OrderViewAssembler;
import com.mallfei.order.application.dto.OrderCancelledEvent;
import com.mallfei.order.application.dto.OrderCreateRequest;
import com.mallfei.order.application.dto.OrderRefundApplyRequest;
import com.mallfei.order.application.dto.OrderTimeoutEvent;
import com.mallfei.order.application.vo.OrderDetailView;
import com.mallfei.order.application.vo.OrderPaidPreviewView;
import com.mallfei.order.application.vo.OrderRefundView;
import com.mallfei.order.application.vo.OrderSummaryView;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.domain.model.OrderRefund;
import com.mallfei.order.domain.repository.OrderRefundRepository;
import com.mallfei.order.domain.repository.ProductSnapshotRepository;
import com.mallfei.order.domain.service.OrderDomainService;
import com.mallfei.product.domain.repository.ProductRepository;
import com.mallfei.product.facade.ProductFacade;
import com.mallfei.stock.application.dto.StockOperationRequest;
import com.mallfei.stock.facade.StockFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class OrderApplicationService {

    private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);
    private static final DateTimeFormatter TIME_LOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final long orderTimeoutMinutes;
    private final OrderDomainService orderDomainService;
    private final OrderRefundRepository orderRefundRepository;
    private final AuthFacade authFacade;
    private final StockFacade stockFacade;
    private final ProductFacade productFacade;
    private final OrderTimeoutEventPublisher orderTimeoutEventPublisher;
    private final OrderEventPublisher orderEventPublisher;
    private final OrderViewAssembler orderViewAssembler;

    public OrderApplicationService(@Value("${mall.order.timeout-minutes:2}") long orderTimeoutMinutes,
                                   OrderDomainService orderDomainService,
                                   OrderRefundRepository orderRefundRepository,
                                   AuthFacade authFacade,
                                   StockFacade stockFacade,
                                   ProductFacade productFacade,
                                   OrderTimeoutEventPublisher orderTimeoutEventPublisher,
                                   OrderEventPublisher orderEventPublisher,
                                   OrderViewAssembler orderViewAssembler) {
        this.orderTimeoutMinutes = orderTimeoutMinutes;
        this.orderDomainService = orderDomainService;
        this.orderRefundRepository = orderRefundRepository;
        this.authFacade = authFacade;
        this.stockFacade = stockFacade;
        this.productFacade = productFacade;
        this.orderTimeoutEventPublisher = orderTimeoutEventPublisher;
        this.orderEventPublisher = orderEventPublisher;
        this.orderViewAssembler = orderViewAssembler;
    }

    public List<OrderSummaryView> currentUserOrders() {
        Long userId = currentUser().principalId();
        autoCancelTimedOutOrders(userId);
        return orderDomainService.loadUserOrders(userId).stream()
                .map(order -> orderViewAssembler.toSummary(order, orderTimeoutMinutes))
                .toList();
    }

    public OrderDetailView currentUserOrder(Long orderId) {
        Order order = orderDomainService.loadOwnedOrder(orderId, currentUser().principalId());
        return orderViewAssembler.toDetail(cancelTimedOutOrderIfNecessary(order), orderTimeoutMinutes);
    }

    public OrderDetailView createOrder(OrderCreateRequest request) {
        String orderNo = newOrderNo();
        Long userId = currentUser().principalId();
        Order order = orderDomainService.createPendingOrder(orderNo, userId, request);
        stockFacade.reserve(new StockOperationRequest("ORDER", orderNo,
                request.items().stream().map(item -> new StockOperationRequest.Item(item.skuId(), item.quantity())).toList()));
        Order persisted = orderDomainService.save(order);
        log.info("Created pending order, orderNo={}, createdAt={}, timeoutMinutes={}, deadline={}",
                persisted.orderNo(),
                formatTime(persisted.createdAtFromOrderNo()),
                orderTimeoutMinutes,
                formatTime(persisted.createdAtFromOrderNo().plusMinutes(orderTimeoutMinutes)));
        publishOrderTimeoutEventQuietly(orderNo);
        return orderViewAssembler.toDetail(persisted, orderTimeoutMinutes);
    }

    public OrderDetailView cancelOrder(Long orderId) {
        Order order = orderDomainService.loadOwnedOrder(orderId, currentUser().principalId());
        Order timeoutChecked = cancelTimedOutOrderIfNecessary(order);
        if (timeoutChecked.timeoutCancelled()) {
            return orderViewAssembler.toDetail(timeoutChecked, orderTimeoutMinutes);
        }
        Order cancelledOrder = timeoutChecked.cancelByUser(LocalDateTime.now());
        orderDomainService.update(cancelledOrder);
        releasePendingOrderStockQuietly(cancelledOrder.orderNo());
        closePendingPayOrdersQuietly(cancelledOrder.orderNo(), Order.STATUS_CANCELLED);
        return orderViewAssembler.toDetail(orderDomainService.loadOrder(orderId), orderTimeoutMinutes);
    }

    public OrderDetailView confirmReceipt(Long orderId) {
        Order order = orderDomainService.loadOwnedOrder(orderId, currentUser().principalId());
        Order completedOrder = order.complete(LocalDateTime.now());
        orderDomainService.update(completedOrder);
        return orderViewAssembler.toDetail(orderDomainService.loadOrder(orderId), orderTimeoutMinutes);
    }

    public OrderRefundView applyRefund(Long orderId, OrderRefundApplyRequest request) {
        Order order = orderDomainService.loadOwnedOrder(orderId, currentUser().principalId());
        Order timeoutChecked = cancelTimedOutOrderIfNecessary(order);
        timeoutChecked.ensureRefundable();
        if (orderRefundRepository.findLatestByOrderNo(timeoutChecked.orderNo()).isPresent()) {
            throw BusinessException.badRequest("该订单已提交退款申请，请勿重复提交");
        }

        OrderRefund refund = orderRefundRepository.save(OrderRefund.create(
                timeoutChecked.id(),
                timeoutChecked.orderNo(),
                timeoutChecked.userId(),
                request.reason().trim()
        ));

        Order refundedOrder = timeoutChecked.refundSuccess(LocalDateTime.now());
        orderDomainService.update(refundedOrder);
        rollbackOrderSalesQuietly(refundedOrder);
        rollbackOrderStockQuietly(refundedOrder);
        OrderRefund successRefund = orderRefundRepository.update(refund.markSuccess());

        return new OrderRefundView(
                successRefund.id(),
                successRefund.refundStatus(),
                successRefund.refundReason(),
                successRefund.createdAt()
        );
    }

    public OrderDetailView markPaid(String orderNo) {
        Order order = cancelTimedOutOrderIfNecessary(orderDomainService.loadOrder(orderNo));
        if (!order.pendingPayment()) {
            return orderViewAssembler.toDetail(order, orderTimeoutMinutes);
        }
        stockFacade.confirm(new StockOperationRequest("ORDER", order.orderNo(), List.of()));
        productFacade.incrementSkuSales(order.items().stream()
                .map(item -> new ProductRepository.SkuSalesIncrement(item.skuId(), item.quantity()))
                .toList());
        Order paidOrder = order.markPaidIfPossible(LocalDateTime.now());
        orderDomainService.update(paidOrder);
        return orderViewAssembler.toDetail(orderDomainService.loadOrder(orderNo), orderTimeoutMinutes);
    }

    public OrderPaidPreviewView markPaidPreview(String orderNo) {
        return orderViewAssembler.toPaidPreview(orderDomainService.loadOrder(orderNo));
    }

    public void autoCancelTimedOutOrders(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        orderDomainService.loadUserOrders(userId).stream()
                .filter(Order::pendingPayment)
                .forEach(order -> logTimeoutDecision("user_order_list_scan", order, now));
        orderDomainService.locateTimedOutOrders(userId, now, orderTimeoutMinutes)
                .forEach(order -> {
                    log.warn("Cancelling timed out order from user scan, orderNo={}, createdAt={}, deadline={}, now={}, timeoutMinutes={}",
                            order.orderNo(),
                            formatTime(order.createdAtFromOrderNo()),
                            formatTime(order.createdAtFromOrderNo().plusMinutes(orderTimeoutMinutes)),
                            formatTime(now),
                            orderTimeoutMinutes);
                    orderDomainService.update(order);
                    releasePendingOrderStockQuietly(order.orderNo());
                    closePendingPayOrdersQuietly(order.orderNo(), Order.STATUS_TIMEOUT_CANCELLED);
                });
    }

    public void autoCancelAllTimedOutOrders() {
        LocalDateTime now = LocalDateTime.now();
        orderDomainService.findAll().stream()
                .filter(Order::pendingPayment)
                .forEach(order -> logTimeoutDecision("schedule_scan", order, now));
        orderDomainService.locateAllTimedOutOrders(now, orderTimeoutMinutes)
                .forEach(order -> {
                    log.warn("Cancelling timed out order from schedule, orderNo={}, createdAt={}, deadline={}, now={}, timeoutMinutes={}",
                            order.orderNo(),
                            formatTime(order.createdAtFromOrderNo()),
                            formatTime(order.createdAtFromOrderNo().plusMinutes(orderTimeoutMinutes)),
                            formatTime(now),
                            orderTimeoutMinutes);
                    orderDomainService.update(order);
                    releasePendingOrderStockQuietly(order.orderNo());
                    closePendingPayOrdersQuietly(order.orderNo(), Order.STATUS_TIMEOUT_CANCELLED);
                });
    }

    public void closeIfTimedOut(String orderNo) {
        LocalDateTime now = LocalDateTime.now();
        Order order = orderDomainService.findByOrderNo(orderNo)
                .map(candidate -> {
                    logTimeoutDecision("mq_close_if_timed_out", candidate, now);
                    return orderDomainService.cancelTimedOutIfNecessary(candidate, now, orderTimeoutMinutes);
                })
                .orElse(null);
        if (order != null && order.timeoutCancelled()) {
            log.warn("Cancelling timed out order from mq event, orderNo={}, createdAt={}, deadline={}, now={}, timeoutMinutes={}",
                    order.orderNo(),
                    formatTime(order.createdAtFromOrderNo()),
                    formatTime(order.createdAtFromOrderNo().plusMinutes(orderTimeoutMinutes)),
                    formatTime(now),
                    orderTimeoutMinutes);
            orderDomainService.update(order);
            releasePendingOrderStockQuietly(order.orderNo());
            closePendingPayOrdersQuietly(order.orderNo(), Order.STATUS_TIMEOUT_CANCELLED);
        }
    }

    private Order cancelTimedOutOrderIfNecessary(Order order) {
        if (!order.pendingPayment()) {
            logTimeoutDecision("request_guard_skip_non_pending", order, LocalDateTime.now());
            return order;
        }
        LocalDateTime now = LocalDateTime.now();
        logTimeoutDecision("request_guard", order, now);
        Order timeoutOrder = orderDomainService.cancelTimedOutIfNecessary(order, now, orderTimeoutMinutes);
        if (timeoutOrder.timeoutCancelled()) {
            log.warn("Cancelling timed out order from request guard, orderNo={}, createdAt={}, deadline={}, now={}, timeoutMinutes={}",
                    timeoutOrder.orderNo(),
                    formatTime(timeoutOrder.createdAtFromOrderNo()),
                    formatTime(timeoutOrder.createdAtFromOrderNo().plusMinutes(orderTimeoutMinutes)),
                    formatTime(now),
                    orderTimeoutMinutes);
            orderDomainService.update(timeoutOrder);
            releasePendingOrderStockQuietly(timeoutOrder.orderNo());
            closePendingPayOrdersQuietly(timeoutOrder.orderNo(), Order.STATUS_TIMEOUT_CANCELLED);
            return orderDomainService.loadOrder(timeoutOrder.id());
        }
        return timeoutOrder;
    }

    private void rollbackOrderSalesQuietly(Order refundedOrder) {
        try {
            productFacade.decrementSkuSales(refundedOrder.items().stream()
                    .map(item -> new ProductRepository.SkuSalesIncrement(item.skuId(), item.quantity()))
                    .toList());
        } catch (Exception exception) {
            log.error("Failed to rollback order sales, orderNo={}", refundedOrder.orderNo(), exception);
        }
    }

    private void rollbackOrderStockQuietly(Order refundedOrder) {
        try {
            stockFacade.restore(new StockOperationRequest(
                    "ORDER_REFUND",
                    refundedOrder.orderNo(),
                    refundedOrder.items().stream()
                            .map(item -> new StockOperationRequest.Item(item.skuId(), item.quantity()))
                            .toList()
            ));
        } catch (Exception exception) {
            log.error("Failed to rollback order stock, orderNo={}", refundedOrder.orderNo(), exception);
        }
    }

    private void releasePendingOrderStockQuietly(String orderNo) {
        try {
            stockFacade.cancel(new StockOperationRequest("ORDER", orderNo, List.of()));
        } catch (Exception exception) {
            log.error("Failed to release pending order stock, orderNo={}", orderNo, exception);
        }
    }

    private void closePendingPayOrdersQuietly(String orderNo, String reasonStatus) {
        try {
            orderEventPublisher.publishCancelled(new OrderCancelledEvent(orderNo, reasonStatus));
        } catch (Exception exception) {
            log.error("Failed to publish cancelled event, orderNo={}", orderNo, exception);
        }
    }

    private void publishOrderTimeoutEventQuietly(String orderNo) {
        try {
            log.info("Publishing order timeout event, orderNo={}, timeoutMinutes={}", orderNo, orderTimeoutMinutes);
            orderTimeoutEventPublisher.publish(new OrderTimeoutEvent(orderNo));
        } catch (Exception exception) {
            log.error("Failed to publish order timeout event, orderNo={}", orderNo, exception);
        }
    }

    private void logTimeoutDecision(String source, Order order, LocalDateTime now) {
        LocalDateTime createdAt = order.createdAtFromOrderNo();
        LocalDateTime deadline = createdAt.plusMinutes(orderTimeoutMinutes);
        boolean timedOut = order.pendingPayment() && now.isAfter(deadline);
        log.info("Order timeout decision, source={}, orderNo={}, status={}, createdAt={}, deadline={}, now={}, timeoutMinutes={}, timedOut={}",
                source,
                order.orderNo(),
                order.orderStatus(),
                formatTime(createdAt),
                formatTime(deadline),
                formatTime(now),
                orderTimeoutMinutes,
                timedOut);
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "-" : time.format(TIME_LOG_FORMATTER);
    }

    private AuthenticatedPrincipal currentUser() {
        AuthenticatedPrincipal principal = authFacade.currentPrincipal();
        if (principal == null || !principal.isUser()) {
            throw BusinessException.forbidden("仅用户可访问当前接口");
        }
        return principal;
    }

    private String newOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
