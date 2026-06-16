package com.mallfei.order.application.service;

import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.common.api.PageResponse;
import com.mallfei.common.api.PageResult;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.application.assembler.OrderViewAssembler;
import com.mallfei.order.application.dto.OrderCancelledEvent;
import com.mallfei.order.application.dto.OrderCreateRequest;
import com.mallfei.order.application.dto.OrderRefundApplyRequest;
import com.mallfei.order.application.dto.OrderRefundFailedEvent;
import com.mallfei.order.application.dto.OrderRefundSucceededEvent;
import com.mallfei.order.application.dto.OrderTimeoutEvent;
import com.mallfei.order.application.vo.OrderDetailView;
import com.mallfei.order.application.vo.OrderPaidPreviewView;
import com.mallfei.order.application.vo.OrderRefundView;
import com.mallfei.order.application.vo.OrderSummaryView;
import com.mallfei.order.config.OrderMqConfig;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.domain.model.OrderItem;
import com.mallfei.order.domain.model.OrderRefund;
import com.mallfei.order.domain.model.OrderRefundItem;
import com.mallfei.order.domain.repository.OrderRefundItemRepository;
import com.mallfei.order.domain.repository.OrderRefundRepository;
import com.mallfei.order.domain.repository.ProductSnapshotRepository;
import com.mallfei.order.domain.service.OrderDomainService;
import com.mallfei.product.domain.repository.ProductRepository;
import com.mallfei.product.facade.ProductFacade;
import com.mallfei.stock.application.dto.StockOperationRequest;
import com.mallfei.stock.facade.StockFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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
    private static final long USER_ORDER_LIST_RECENT_DAYS = 10L;

    private final long orderTimeoutMinutes;
    private final OrderDomainService orderDomainService;
    private final OrderRefundRepository orderRefundRepository;
    private final OrderRefundItemRepository orderRefundItemRepository;
    private final AuthFacade authFacade;
    private final StockFacade stockFacade;
    private final ProductFacade productFacade;
    private final OrderTimeoutEventPublisher orderTimeoutEventPublisher;
    private final OrderEventPublisher orderEventPublisher;
    private final OrderViewAssembler orderViewAssembler;

    public OrderApplicationService(@Value("${mall.order.timeout-minutes:2}") long orderTimeoutMinutes,
                                   OrderDomainService orderDomainService,
                                   OrderRefundRepository orderRefundRepository,
                                   OrderRefundItemRepository orderRefundItemRepository,
                                   AuthFacade authFacade,
                                   StockFacade stockFacade,
                                   ProductFacade productFacade,
                                   OrderTimeoutEventPublisher orderTimeoutEventPublisher,
                                   OrderEventPublisher orderEventPublisher,
                                   OrderViewAssembler orderViewAssembler) {
        this.orderTimeoutMinutes = orderTimeoutMinutes;
        this.orderDomainService = orderDomainService;
        this.orderRefundRepository = orderRefundRepository;
        this.orderRefundItemRepository = orderRefundItemRepository;
        this.authFacade = authFacade;
        this.stockFacade = stockFacade;
        this.productFacade = productFacade;
        this.orderTimeoutEventPublisher = orderTimeoutEventPublisher;
        this.orderEventPublisher = orderEventPublisher;
        this.orderViewAssembler = orderViewAssembler;
    }

    public List<OrderSummaryView> currentUserOrders() {
        return currentUserOrders(1, 10, "ALL", null).records();
    }

    public PageResponse<OrderSummaryView> currentUserOrders(long page, long size, String status, String keyword) {
        Long userId = currentUser().principalId();
        closeRecentTimedOutOrdersForUser(userId);
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), 50);
        LocalDateTime createdAfter = LocalDateTime.now().minusDays(USER_ORDER_LIST_RECENT_DAYS);
        PageResult<Order> pageResult = orderDomainService.pageUserOrders(userId, status, keyword, createdAfter, safePage, safeSize);
        List<OrderSummaryView> records = pageResult.records().stream()
                .map(order -> orderViewAssembler.toSummary(order, orderTimeoutMinutes, orderRefundRepository.findLatestByOrderNo(order.orderNo()).orElse(null)))
                .toList();
        return new PageResponse<>(records, pageResult.total(), safePage, safeSize);
    }

    public OrderDetailView currentUserOrder(Long orderId) {
        Order order = orderDomainService.loadOwnedOrder(orderId, currentUser().principalId());
        return orderViewAssembler.toDetail(cancelTimedOutOrderIfNecessary(order), orderTimeoutMinutes);
    }

    public OrderDetailView createOrder(OrderCreateRequest request) {
        String orderNo = newOrderNo();
        Long userId = currentUser().principalId();
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(orderTimeoutMinutes);
        Order order = orderDomainService.createPendingOrder(orderNo, userId, request, expireTime);
        boolean stockReserved = false;
        try {
            stockFacade.reserve(new StockOperationRequest("ORDER", orderNo,
                    request.items().stream().map(item -> new StockOperationRequest.Item(item.skuId(), item.quantity())).toList()));
            stockReserved = true;
            Order persisted = orderDomainService.save(order);
            log.debug("Created pending order, orderNo={}, createdAt={}, timeoutMinutes={}, deadline={}",
                    persisted.orderNo(),
                    formatTime(persisted.createdAtFromOrderNo()),
                    orderTimeoutMinutes,
                    formatTime(persisted.effectiveExpireTime(orderTimeoutMinutes)));
            publishOrderTimeoutEventQuietly(orderNo, persisted.effectiveExpireTime(orderTimeoutMinutes));
            return orderViewAssembler.toDetail(persisted, orderTimeoutMinutes);
        } catch (Exception exception) {
            if (stockReserved) {
                releasePendingOrderStockAfterCreateFailure(orderNo, exception);
            }
            throw exception;
        }
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
        if (completedOrder.shouldIncrementSalesAfterCompleted(order)) {
            incrementOrderSalesQuietly(completedOrder);
        }
        return orderViewAssembler.toDetail(orderDomainService.loadOrder(orderId), orderTimeoutMinutes);
    }

    public void deleteCurrentUserOrder(Long orderId) {
        Long userId = currentUser().principalId();
        Order order = orderDomainService.loadOwnedOrder(orderId, userId);
        Order timeoutChecked = cancelTimedOutOrderIfNecessary(order);
        if (!terminalOrder(timeoutChecked)) {
            throw BusinessException.badRequest("仅已完成、已退款、已取消等终态订单可以删除");
        }
        orderDomainService.markUserDeleted(orderId, userId);
    }

    public OrderRefundView applyRefund(Long orderId, OrderRefundApplyRequest request) {
        Order order = orderDomainService.loadOwnedOrder(orderId, currentUser().principalId());
        Order timeoutChecked = cancelTimedOutOrderIfNecessary(order);
        return createRefund(timeoutChecked, request, RefundSubmitPolicy.SUBMIT_TO_PAY_CHANNEL);
    }

    public OrderRefundView applyRefundByAdmin(Order order, OrderRefundApplyRequest request) {
        return createRefund(order, request, RefundSubmitPolicy.SUBMIT_TO_PAY_CHANNEL);
    }

    public OrderRefundView createRefundApplicationByAdmin(Order order, OrderRefundApplyRequest request) {
        return createRefund(order, request, RefundSubmitPolicy.WAIT_FOR_AFTERSALE_AUDIT);
    }

    public OrderRefundView approveRefundApplicationByAdmin(String refundNo) {
        OrderRefund refunding = orderRefundRepository.submitForRefunding(refundNo);
        orderEventPublisher.publishRefundRequested(refunding.orderNo(), refunding.refundAmountCent(), refunding.refundReason(), refunding.refundNo());
        return toRefundView(refunding);
    }

    private OrderRefundView createRefund(Order order, OrderRefundApplyRequest request, RefundSubmitPolicy submitPolicy) {
        order.ensureRefundable();
        ensureNoRefundInProgress(order.orderNo());

        String refundNo = newRefundNo();
        List<OrderRefundItem> refundItems = buildRefundItems(order, request, refundNo);
        long refundAmountCent = refundItems.stream()
                .mapToLong(item -> item.refundAmountCent() == null ? 0L : item.refundAmountCent())
                .sum();
        if (refundAmountCent <= 0) {
            throw BusinessException.badRequest("退款金额必须大于0");
        }
        OrderRefund refund = orderRefundRepository.save(OrderRefund.create(
                order.id(),
                order.orderNo(),
                order.userId(),
                refundNo,
                refundAmountCent,
                request.reason().trim()
        ));
        orderRefundItemRepository.saveBatch(refundItems.stream()
                .map(item -> item.bindRefundId(refund.id()))
                .toList());

        if (submitPolicy == RefundSubmitPolicy.WAIT_FOR_AFTERSALE_AUDIT) {
            log.info("Created admin negotiated refund application without submitting pay refund, orderNo={}, refundNo={}, amountCent={}",
                    order.orderNo(), refund.refundNo(), refund.refundAmountCent());
            return toRefundView(refund);
        }

        OrderRefund refunding = orderRefundRepository.update(refund.markRefunding());
        orderEventPublisher.publishRefundRequested(refunding.orderNo(), refunding.refundAmountCent(), request.reason().trim(), refunding.refundNo());

        return toRefundView(refunding);
    }

    @RabbitListener(queues = OrderMqConfig.ORDER_REFUND_SUCCEEDED_QUEUE)
    public void onRefundSucceeded(OrderRefundSucceededEvent event) {
        Order order = orderDomainService.loadOrder(event.orderNo());
        if (order.refunded()) {
            log.info("Ignoring duplicated refund succeeded event, orderNo={}, refundNo={}", event.orderNo(), event.refundNo());
            return;
        }
        OrderRefund refund = orderRefundRepository.findLatestByOrderNo(event.orderNo())
                .orElseThrow(() -> BusinessException.badRequest("退款申请不存在"));
        ensureRefundEventMatches(refund, event.refundNo(), event.refundAmountCent());
        List<OrderRefundItem> refundItems = orderRefundItemRepository.findByRefundNo(event.refundNo());
        Order refundedOrder = refund.fullRefund(order.safePayAmountCent())
                ? order.refundSuccess(LocalDateTime.now())
                : order.partialRefundSuccess(LocalDateTime.now());
        orderDomainService.update(refundedOrder);
        if (refundedOrder.shouldRollbackSalesAfterRefundSuccess(order)) {
            rollbackOrderSalesQuietly(refundedOrder, refundItems);
        }
        rollbackOrderStockQuietly(event.refundNo(), refundItems);
        orderRefundRepository.update(refund.markSuccess(event.channelRefundNo()));
        log.info("Marked order refund success from pay event, orderNo={}, refundNo={}, channelRefundNo={}",
                event.orderNo(), event.refundNo(), event.channelRefundNo());
    }

    @RabbitListener(queues = OrderMqConfig.ORDER_REFUND_FAILED_QUEUE)
    public void onRefundFailed(OrderRefundFailedEvent event) {
        OrderRefund refund = orderRefundRepository.findLatestByOrderNo(event.orderNo())
                .orElseThrow(() -> BusinessException.badRequest("退款申请不存在"));
        ensureRefundEventMatches(refund, event.refundNo(), event.refundAmountCent());
        if (refund.success()) {
            log.warn("Ignoring refund failed event because refund already succeeded, orderNo={}, refundNo={}", event.orderNo(), event.refundNo());
            return;
        }
        orderRefundRepository.update(refund.markFailed(event.failReason()));
        log.warn("Marked order refund failed from pay event, orderNo={}, refundNo={}, reason={}",
                event.orderNo(), event.refundNo(), event.failReason());
    }

    public OrderDetailView markPaid(String orderNo) {
        Order order = cancelTimedOutOrderIfNecessary(orderDomainService.loadOrder(orderNo));
        if (order.paymentException()) {
            log.warn("Skipped automatic paid transition because order is marked payment exception, orderNo={}", order.orderNo());
            return orderViewAssembler.toDetail(order, orderTimeoutMinutes);
        }
        if (!order.pendingPayment()) {
            return orderViewAssembler.toDetail(order, orderTimeoutMinutes);
        }
        stockFacade.confirm(new StockOperationRequest("ORDER", order.orderNo(), List.of()));
        orderDomainService.markPaid(order.orderNo(), LocalDateTime.now());
        Order latestOrder = orderDomainService.loadOrder(orderNo);
        return orderViewAssembler.toDetail(latestOrder, orderTimeoutMinutes);
    }

    public OrderPaidPreviewView markPaidPreview(String orderNo) {
        return orderViewAssembler.toPaidPreview(orderDomainService.loadOrder(orderNo));
    }

    public void completeOrderBySystem(String orderNo) {
        Order order = orderDomainService.loadOrder(orderNo);
        Order completedOrder = order.complete(LocalDateTime.now());
        orderDomainService.update(completedOrder);
        if (completedOrder.shouldIncrementSalesAfterCompleted(order)) {
            incrementOrderSalesQuietly(completedOrder);
        }
    }

    public void autoConfirmShippedOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(7);
        orderDomainService.findAll().stream()
                .filter(order -> Order.STATUS_SHIPPED.equals(order.orderStatus()))
                .filter(order -> order.shippedAt() != null && !order.shippedAt().isAfter(deadline))
                .forEach(order -> {
                    try {
                        completeOrderBySystem(order.orderNo());
                    } catch (Exception exception) {
                        log.error("Failed to auto confirm shipped order, orderNo={}", order.orderNo(), exception);
                    }
                });
    }

    public void autoCancelTimedOutOrders(Long userId) {
        closeRecentTimedOutOrdersForUser(userId);
    }

    private void closeRecentTimedOutOrdersForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        orderDomainService.findTimedOutPendingOrdersByUserId(userId, now, 20)
                .forEach(order -> {
                    logTimeoutDecision("user_order_list_scan", order, now);
                    closeTimedOutOrder(order.orderNo(), "user scan", now);
                });
    }

    public void autoCancelAllTimedOutOrders() {
        LocalDateTime now = LocalDateTime.now();
        orderDomainService.findTimedOutPendingOrders(now, 200)
                .forEach(order -> {
                    logTimeoutDecision("schedule_scan", order, now);
                    closeTimedOutOrder(order.orderNo(), "schedule", now);
                });
    }

    public void closeIfTimedOut(String orderNo) {
        LocalDateTime now = LocalDateTime.now();
        orderDomainService.findByOrderNo(orderNo)
                .ifPresent(order -> logTimeoutDecision("mq_close_if_timed_out", order, now));
        closeTimedOutOrder(orderNo, "mq event", now);
    }

    public boolean closeIfTimedOutForFacade(String orderNo) {
        return closeTimedOutOrder(orderNo, "facade guard", LocalDateTime.now());
    }

    private boolean terminalOrder(Order order) {
        return Order.STATUS_COMPLETED.equals(order.orderStatus())
                || Order.STATUS_REFUNDED.equals(order.orderStatus())
                || Order.STATUS_PARTIALLY_REFUNDED.equals(order.orderStatus())
                || Order.STATUS_CANCELLED.equals(order.orderStatus())
                || Order.STATUS_TIMEOUT_CANCELLED.equals(order.orderStatus())
                || Order.STATUS_CLOSED.equals(order.orderStatus())
                || Order.STATUS_REFUND_CLOSED.equals(order.orderStatus());
    }

    private Order cancelTimedOutOrderIfNecessary(Order order) {
        if (!order.pendingPayment()) {
            logTimeoutDecision("request_guard_skip_non_pending", order, LocalDateTime.now());
            return order;
        }
        LocalDateTime now = LocalDateTime.now();
        logTimeoutDecision("request_guard", order, now);
        if (order.timedOut(now)) {
            closeTimedOutOrder(order.orderNo(), "request guard", now);
            return orderDomainService.loadOrder(order.id());
        }
        return order;
    }

    private boolean closeTimedOutOrder(String orderNo, String source, LocalDateTime now) {
        boolean closed = orderDomainService.closeTimedOut(orderNo, now);
        if (!closed) {
            return false;
        }
        Order closedOrder = orderDomainService.loadOrder(orderNo);
        log.warn("Cancelling timed out order from {}, orderNo={}, createdAt={}, deadline={}, now={}, timeoutMinutes={}",
                source,
                closedOrder.orderNo(),
                formatTime(closedOrder.createdAtFromOrderNo()),
                formatTime(closedOrder.effectiveExpireTime(orderTimeoutMinutes)),
                formatTime(now),
                orderTimeoutMinutes);
        releasePendingOrderStockQuietly(orderNo);
        closePendingPayOrdersQuietly(orderNo, Order.STATUS_TIMEOUT_CANCELLED);
        return true;
    }

    private void incrementOrderSalesQuietly(Order completedOrder) {
        try {
            productFacade.incrementSkuSales(completedOrder.items().stream()
                    .map(item -> new ProductRepository.SkuSalesIncrement(item.skuId(), item.quantity()))
                    .toList());
            productFacade.recordOrderCompletedSales(completedOrder.orderNo(), completedOrder.completedAt(), completedOrder.items().stream()
                    .map(item -> new ProductFacade.ProductSalesItem(item.spuId(), item.skuId(), item.quantity(), item.totalAmountCent()))
                    .toList());
        } catch (Exception exception) {
            log.error("Failed to increment order sales, orderNo={}", completedOrder.orderNo(), exception);
        }
    }

    private void rollbackOrderSalesQuietly(Order refundedOrder, List<OrderRefundItem> refundItems) {
        try {
            productFacade.decrementSkuSales(refundItems.stream()
                    .map(item -> new ProductRepository.SkuSalesIncrement(item.skuId(), item.quantity()))
                    .toList());
        } catch (Exception exception) {
            log.error("Failed to rollback order sales, orderNo={}", refundedOrder.orderNo(), exception);
        }
    }

    private void rollbackOrderStockQuietly(String refundNo, List<OrderRefundItem> refundItems) {
        try {
            stockFacade.restore(new StockOperationRequest(
                    "ORDER_REFUND",
                    refundNo,
                    refundItems.stream()
                            .map(item -> new StockOperationRequest.Item(item.skuId(), item.quantity()))
                            .toList()
            ));
        } catch (Exception exception) {
            log.error("Failed to rollback order stock, refundNo={}", refundNo, exception);
        }
    }

    private void releasePendingOrderStockQuietly(String orderNo) {
        try {
            stockFacade.cancel(new StockOperationRequest("ORDER", orderNo, List.of()));
        } catch (Exception exception) {
            log.error("Failed to release pending order stock, orderNo={}", orderNo, exception);
        }
    }

    private void releasePendingOrderStockAfterCreateFailure(String orderNo, Exception createException) {
        try {
            stockFacade.cancel(new StockOperationRequest("ORDER", orderNo, List.of()));
        } catch (Exception releaseException) {
            createException.addSuppressed(releaseException);
            log.error("Failed to release reserved stock after order creation failure, orderNo={}", orderNo, releaseException);
        }
    }

    private void closePendingPayOrdersQuietly(String orderNo, String reasonStatus) {
        try {
            orderEventPublisher.publishCancelled(new OrderCancelledEvent(orderNo, reasonStatus));
        } catch (Exception exception) {
            log.error("Failed to publish cancelled event, orderNo={}", orderNo, exception);
        }
    }

    private void publishOrderTimeoutEventQuietly(String orderNo, LocalDateTime expireTime) {
        try {
            log.debug("Publishing order timeout event, orderNo={}, timeoutMinutes={}, expireTime={}", orderNo, orderTimeoutMinutes, formatTime(expireTime));
            orderTimeoutEventPublisher.publish(new OrderTimeoutEvent(orderNo), expireTime);
        } catch (Exception exception) {
            log.error("Failed to publish order timeout event, orderNo={}", orderNo, exception);
        }
    }

    private void logTimeoutDecision(String source, Order order, LocalDateTime now) {
        LocalDateTime createdAt = order.createdAtFromOrderNo();
        LocalDateTime deadline = order.effectiveExpireTime(orderTimeoutMinutes);
        boolean timedOut = order.pendingPayment() && !now.isBefore(deadline);
        log.debug("Order timeout decision, source={}, orderNo={}, status={}, createdAt={}, deadline={}, now={}, timeoutMinutes={}, timedOut={}",
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

    private List<OrderRefundItem> buildRefundItems(Order order, OrderRefundApplyRequest request, String refundNo) {
        List<OrderRefund> succeededRefunds = orderRefundRepository.findByOrderNo(order.orderNo()).stream()
                .filter(OrderRefund::success)
                .toList();
        List<OrderRefundItem> succeededItems = succeededRefunds.stream()
                .flatMap(refund -> orderRefundItemRepository.findByRefundNo(refund.refundNo()).stream())
                .toList();
        if (request.items() == null || request.items().isEmpty()) {
            return order.items().stream()
                    .map(item -> OrderRefundItem.create(refundNo, item, remainingRefundQuantity(item, succeededItems)))
                    .filter(item -> item.quantity() > 0)
                    .toList();
        }
        return request.items().stream()
                .map(item -> {
                    OrderItem orderItem = order.items().stream()
                            .filter(origin -> item.orderItemId() != null && item.orderItemId().equals(origin.id())
                                    || item.skuId() != null && item.skuId().equals(origin.skuId()))
                            .findFirst()
                            .orElseThrow(() -> BusinessException.badRequest("退款明细不存在于订单中"));
                    int remainingQuantity = remainingRefundQuantity(orderItem, succeededItems);
                    if (item.quantity() == null || item.quantity() <= 0) {
                        throw BusinessException.badRequest("退款数量必须大于0");
                    }
                    if (item.quantity() > remainingQuantity) {
                        throw BusinessException.badRequest("退款数量超过剩余可退数量");
                    }
                    return OrderRefundItem.create(refundNo, orderItem, item.quantity());
                })
                .toList();
    }

    private int remainingRefundQuantity(OrderItem orderItem, List<OrderRefundItem> succeededItems) {
        int purchasedQuantity = orderItem.quantity() == null ? 0 : orderItem.quantity();
        int refundedQuantity = succeededItems.stream()
                .filter(item -> orderItem.id() != null && orderItem.id().equals(item.orderItemId()))
                .mapToInt(item -> item.quantity() == null ? 0 : item.quantity())
                .sum();
        return Math.max(0, purchasedQuantity - refundedQuantity);
    }

    private void ensureNoRefundInProgress(String orderNo) {
        boolean hasRefundInProgress = orderRefundRepository.findByOrderNo(orderNo).stream()
                .anyMatch(refund -> OrderRefund.STATUS_PENDING.equals(refund.refundStatus()) || refund.refunding());
        if (hasRefundInProgress) {
            throw BusinessException.badRequest("该订单存在处理中的退款申请，请勿重复提交");
        }
    }

    private void ensureRefundEventMatches(OrderRefund refund, String refundNo, Long refundAmountCent) {
        if (!refund.refundNo().equals(refundNo)) {
            throw BusinessException.badRequest("退款单号不匹配");
        }
        if (refundAmountCent != null && refund.refundAmountCent() != null && !refund.refundAmountCent().equals(refundAmountCent)) {
            throw BusinessException.badRequest("退款金额不匹配");
        }
    }

    private OrderRefundView toRefundView(OrderRefund refund) {
        return new OrderRefundView(
                refund.id(),
                refund.refundNo(),
                refund.refundAmountCent(),
                refund.channelRefundNo(),
                refund.refundStatus(),
                refund.refundReason(),
                refund.failReason(),
                refund.createdAt()
        );
    }

    private enum RefundSubmitPolicy {
        SUBMIT_TO_PAY_CHANNEL,
        WAIT_FOR_AFTERSALE_AUDIT
    }

    private AuthenticatedPrincipal currentUser() {
        AuthenticatedPrincipal principal = authFacade.currentRequiredPrincipal();
        if (principal == null || !principal.isUser()) {
            throw BusinessException.forbidden("仅用户可访问当前接口");
        }
        return principal;
    }

    private String newOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String newRefundNo() {
        return "ORF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
