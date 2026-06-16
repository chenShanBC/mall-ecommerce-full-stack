package com.mallfei.order.application.service;

import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.application.dto.OrderCancelledEvent;
import com.mallfei.order.application.dto.OrderRefundApplyRequest;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.domain.model.OrderItem;
import com.mallfei.order.domain.model.ProductSnapshot;
import com.mallfei.order.domain.repository.OrderRepository;
import com.mallfei.order.domain.service.OrderDomainService;
import com.mallfei.stock.application.dto.StockOperationRequest;
import com.mallfei.stock.application.service.StockApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderAdminOperationService {

    private final OrderDomainService orderDomainService;
    private final OrderRepository orderRepository;
    private final OrderApplicationService orderApplicationService;
    private final StockApplicationService stockApplicationService;
    private final OrderEventPublisher orderEventPublisher;

    public OrderAdminOperationService(OrderDomainService orderDomainService,
                                      OrderRepository orderRepository,
                                      OrderApplicationService orderApplicationService,
                                      StockApplicationService stockApplicationService,
                                      OrderEventPublisher orderEventPublisher) {
        this.orderDomainService = orderDomainService;
        this.orderRepository = orderRepository;
        this.orderApplicationService = orderApplicationService;
        this.stockApplicationService = stockApplicationService;
        this.orderEventPublisher = orderEventPublisher;
    }

    public void cancel(String orderNo) {
        Order order = orderDomainService.loadOrder(orderNo);
        Order updated = order.cancelByAdmin(LocalDateTime.now());
        orderDomainService.update(updated);
        Order latest = orderDomainService.loadOrder(orderNo);
        if (!Order.STATUS_CANCELLED.equals(latest.orderStatus())) {
            throw new IllegalStateException("订单关闭后校验失败，当前状态=" + latest.orderStatus() + "，请检查订单更新是否生效");
        }
        if (latest.shouldReleaseStockAfterCancelled(order)) {
            stockApplicationService.cancel(new StockOperationRequest("ORDER", order.orderNo(), List.of()));
            orderEventPublisher.publishCancelled(new OrderCancelledEvent(order.orderNo(), Order.STATUS_CANCELLED));
        }
    }

    public void ship(String orderNo) {
        Order order = orderDomainService.loadOrder(orderNo);
        orderDomainService.update(order.ship(LocalDateTime.now()));
    }

    public void complete(String orderNo) {
        orderApplicationService.completeOrderBySystem(orderNo);
    }

    public Order reviseReceiver(String orderNo, String receiverName, String receiverPhone, String receiverProvinceName, String receiverCityName, String receiverDistrictName, String receiverDetailAddress, String note) {
        Order order = orderDomainService.loadOrder(orderNo);
        Order target = order.reviseReceiver(receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress);
        String normalizedNote = note == null || note.isBlank() ? null : note.trim();
        orderDomainService.updateReceiverAddress(target.orderNo(), target.receiverName(), target.receiverPhone(), target.receiverProvinceName(), target.receiverCityName(), target.receiverDistrictName(), target.receiverDetailAddress(), normalizedNote);
        Order latest = orderDomainService.loadOrder(orderNo);
        if (!target.hasSameReceiver(latest)) {
            throw new IllegalStateException("订单收货地址更新后校验失败，请检查数据库字段映射");
        }
        return latest;
    }

    public int markPaymentException(String orderNo, String note) {
        return orderDomainService.markPaymentException(orderNo, note);
    }

    public void confirmPaid(String orderNo, String note) {
        Order order = orderDomainService.loadOrder(orderNo);
        orderDomainService.markPaidByAdmin(orderNo, LocalDateTime.now(), note);
        Order paidOrder = orderDomainService.loadOrder(orderNo);
        if (paidOrder.shouldConfirmStockAfterPaid(order)) {
            stockApplicationService.confirm(new StockOperationRequest("ORDER", order.orderNo(), List.of()));
        }
    }

    public void restorePendingPayment(String orderNo, String note) {
        orderDomainService.restorePendingPayment(orderNo, note);
    }

    @Transactional(rollbackFor = Exception.class)
    public Order createNegotiatedReturnRefund(String orderNo, String note) {
        Order order = orderDomainService.loadOrder(orderNo);
        order.ensureNegotiatedReturnAllowed();
        OrderRefundApplyRequest request = new OrderRefundApplyRequest(normalizeReason(note, "用户协商申请退货"), List.of());
        orderApplicationService.createRefundApplicationByAdmin(order, request);
        Order target = orderDomainService.loadOrder(orderNo).keepStatusWithNegotiatedRefundApplication(note);
        orderDomainService.update(target);
        return orderDomainService.loadOrder(orderNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public Order switchSkuByNegotiation(String orderNo, Long orderItemId, ProductSnapshot targetSnapshot, String priceDifferenceHandleType, String note) {
        if (orderItemId == null) {
            throw BusinessException.badRequest("请选择需要切换的订单商品项");
        }
        if (targetSnapshot == null || targetSnapshot.skuId() == null) {
            throw BusinessException.badRequest("请选择目标SKU");
        }
        Order order = orderDomainService.loadOrder(orderNo);
        order.ensureSkuSwitchable();
        if (!"SAME_PRICE".equalsIgnoreCase(priceDifferenceHandleType == null ? "SAME_PRICE" : priceDifferenceHandleType.trim())) {
            throw BusinessException.badRequest("第一版仅支持同价换SKU，差价请先线下处理后选择同价换SKU");
        }
        OrderItem sourceItem = order.items().stream()
                .filter(item -> item.id() != null && item.id().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> BusinessException.badRequest("订单商品项不存在"));
        if (sourceItem.skuId().equals(targetSnapshot.skuId())) {
            throw BusinessException.badRequest("目标SKU不能与原SKU相同");
        }
        if (!sourceItem.salePriceCent().equals(targetSnapshot.salePriceCent())) {
            throw BusinessException.badRequest("当前仅支持同价SKU替换，存在差价请先走补差/退款流程");
        }
        stockApplicationService.restore(new StockOperationRequest("ORDER_SKU_SWITCH", orderNo + "_OLD_" + sourceItem.skuId(), List.of(new StockOperationRequest.Item(sourceItem.skuId(), sourceItem.quantity()))));
        stockApplicationService.deduct(new StockOperationRequest("ORDER_SKU_SWITCH", orderNo + "_NEW_" + targetSnapshot.skuId(), List.of(new StockOperationRequest.Item(targetSnapshot.skuId(), sourceItem.quantity()))));
        String remark = "NEGOTIATION_SWITCH_SKU: itemId=" + orderItemId + ", oldSku=" + sourceItem.skuId() + ", newSku=" + targetSnapshot.skuId() + ", qty=" + sourceItem.quantity() + (note == null || note.isBlank() ? "" : ", note=" + note.trim());
        orderRepository.replaceOrderItem(order, new OrderRepository.OrderItemReplacement(orderItemId, targetSnapshot.skuId(), targetSnapshot.spuId(), targetSnapshot.skuName(), targetSnapshot.skuImageUrl(), targetSnapshot.salePriceCent(), remark));
        return orderDomainService.loadOrder(orderNo);
    }

    public Order returnToPaidForLogisticsException(String orderNo, String note) {
        Order order = orderDomainService.loadOrder(orderNo);
        Order target = order.returnToPaidForLogisticsException(note);
        orderDomainService.update(target);
        return orderDomainService.loadOrder(orderNo);
    }

    private String normalizeReason(String note, String defaultReason) {
        return note == null || note.isBlank() ? defaultReason : note.trim();
    }
}
