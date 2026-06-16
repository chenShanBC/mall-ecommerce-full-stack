package com.mallfei.order.facade;

import com.mallfei.common.api.PageResult;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.application.dto.OrderCreateRequest;
import com.mallfei.order.application.service.OrderAdminOperationService;
import com.mallfei.order.application.service.OrderApplicationService;
import com.mallfei.order.application.vo.OrderDetailView;
import com.mallfei.order.application.vo.OrderRefundView;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.domain.model.OrderRefund;
import com.mallfei.order.domain.model.OrderRefundItem;
import com.mallfei.order.domain.model.ProductSnapshot;
import com.mallfei.order.domain.repository.OrderRefundItemRepository;
import com.mallfei.order.domain.repository.OrderRefundRepository;
import com.mallfei.order.domain.repository.ProductSnapshotRepository;
import com.mallfei.order.domain.service.OrderDomainService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class OrderFacade {

    private final OrderDomainService orderDomainService;
    private final OrderApplicationService orderApplicationService;
    private final OrderAdminOperationService orderAdminOperationService;
    private final OrderRefundRepository orderRefundRepository;
    private final OrderRefundItemRepository orderRefundItemRepository;
    private final ProductSnapshotRepository productSnapshotRepository;

    public OrderFacade(OrderDomainService orderDomainService,
                       OrderApplicationService orderApplicationService,
                       OrderAdminOperationService orderAdminOperationService,
                       OrderRefundRepository orderRefundRepository,
                       OrderRefundItemRepository orderRefundItemRepository,
                       ProductSnapshotRepository productSnapshotRepository) {
        this.orderDomainService = orderDomainService;
        this.orderApplicationService = orderApplicationService;
        this.orderAdminOperationService = orderAdminOperationService;
        this.orderRefundRepository = orderRefundRepository;
        this.orderRefundItemRepository = orderRefundItemRepository;
        this.productSnapshotRepository = productSnapshotRepository;
    }

    public List<Order> findAll() { return orderDomainService.findAll(); }
    public PageResult<Order> search(String status, String keyword, long page, long size) { return orderDomainService.search(status, keyword, page, size); }
    public PageResult<Order> search(String status, String keyword, long page, long size, String sortBy, String sortOrder) { return orderDomainService.search(status, keyword, page, size, sortBy, sortOrder); }
    public PageResult<Order> search(String status, String keyword, java.time.LocalDate startDate, java.time.LocalDate endDate, long page, long size, String sortBy, String sortOrder) { return orderDomainService.search(status, keyword, startDate, endDate, page, size, sortBy, sortOrder); }
    public long countAll() { return orderDomainService.countAll(); }
    public long countByStatus(String status) { return orderDomainService.countByStatus(status); }
    public long countCancelled() { return orderDomainService.countCancelled(); }
    public long sumPaidAmount() { return orderDomainService.sumPaidAmount(); }
    public Optional<Order> findByOrderNo(String orderNo) { return orderDomainService.findByOrderNo(orderNo); }
    public Order getByOrderNo(String orderNo) { return orderDomainService.loadOrder(orderNo); }
    public List<OrderRefund> refundsByOrderNo(String orderNo) { return orderRefundRepository.findByOrderNo(orderNo); }
    public List<OrderRefund> searchRefunds(String status, String keyword) { return orderRefundRepository.search(status, keyword); }
    public OrderRefund getRefundByRefundNo(String refundNo) { return orderRefundRepository.findByRefundNo(refundNo).orElseThrow(() -> BusinessException.badRequest("退款单不存在: " + refundNo)); }
    public OrderRefundView approveRefundApplicationByAdmin(String refundNo) { return orderApplicationService.approveRefundApplicationByAdmin(refundNo); }
    public OrderRefundView createRefundApplicationByAdmin(Order order, com.mallfei.order.application.dto.OrderRefundApplyRequest request) { return orderApplicationService.createRefundApplicationByAdmin(order, request); }
    public List<OrderRefundItem> refundItemsByRefundNo(String refundNo) { return orderRefundItemRepository.findByRefundNo(refundNo); }
    public Order getOwnedOrder(Long orderId, Long userId) { return orderDomainService.loadOwnedOrder(orderId, userId); }
    public List<Order> getUserOrders(Long userId) { return orderDomainService.loadUserOrders(userId); }
    public ProductSnapshot getProductSnapshot(Long skuId) { return productSnapshotRepository.findBySkuId(skuId).orElseThrow(() -> BusinessException.badRequest("商品SKU不存在: " + skuId)); }
    public OrderDetailView createOrder(OrderCreateRequest request) { return orderApplicationService.createOrder(request); }
    public OrderDetailView markPaid(String orderNo) { return orderApplicationService.markPaid(orderNo); }
    public boolean closeIfTimedOut(String orderNo) { return orderApplicationService.closeIfTimedOutForFacade(orderNo); }
    public void markRefundPending(String orderNo) {
        Order order = orderDomainService.loadOrder(orderNo);
        orderDomainService.update(order.markRefundPending());
    }
    public void rejectRefundToCompleted(String orderNo, String note) {
        rejectRefundToStatus(orderNo, Order.STATUS_COMPLETED, note);
    }
    public void rejectRefundToStatus(String orderNo, String targetStatus, String note) {
        Order order = orderDomainService.loadOrder(orderNo);
        orderDomainService.update(order.restoreFromRefundRejected(targetStatus, note));
    }
    public void markRefundSuccess(String orderNo) {
        Order order = orderDomainService.loadOrder(orderNo);
        orderDomainService.update(order.refundSuccess(java.time.LocalDateTime.now()));
    }
    public void ensureRefundable(String orderNo) { orderDomainService.loadOrder(orderNo).ensureRefundable(); }
    public void cancelByAdmin(String orderNo) { orderAdminOperationService.cancel(orderNo); }
    public void shipByAdmin(String orderNo) { orderAdminOperationService.ship(orderNo); }
    public void completeByAdmin(String orderNo) { orderAdminOperationService.complete(orderNo); }
    public Order reviseReceiverByAdmin(String orderNo, String receiverName, String receiverPhone, String receiverProvinceName, String receiverCityName, String receiverDistrictName, String receiverDetailAddress, String note) { return orderAdminOperationService.reviseReceiver(orderNo, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, note); }
    public int markPaymentExceptionByAdmin(String orderNo, String note) { return orderAdminOperationService.markPaymentException(orderNo, note); }
    public void confirmPaidByAdmin(String orderNo, String note) { orderAdminOperationService.confirmPaid(orderNo, note); }
    public void repairPaidByAdmin(String orderNo, String note) { orderAdminOperationService.confirmPaid(orderNo, note); }
    public void restorePendingPaymentByAdmin(String orderNo, String note) { orderAdminOperationService.restorePendingPayment(orderNo, note); }
    public Order createNegotiatedReturnRefundByAdmin(String orderNo, String note) { return orderAdminOperationService.createNegotiatedReturnRefund(orderNo, note); }
    public Order switchSkuByNegotiationByAdmin(String orderNo, Long orderItemId, Long targetSkuId, String priceDifferenceHandleType, String note) { return orderAdminOperationService.switchSkuByNegotiation(orderNo, orderItemId, getProductSnapshot(targetSkuId), priceDifferenceHandleType, note); }
    public Order returnToPaidForLogisticsExceptionByAdmin(String orderNo, String note) { return orderAdminOperationService.returnToPaidForLogisticsException(orderNo, note); }
}
