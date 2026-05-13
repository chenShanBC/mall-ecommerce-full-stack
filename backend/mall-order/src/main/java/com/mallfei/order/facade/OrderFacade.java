package com.mallfei.order.facade;

import com.mallfei.common.api.PageResult;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.application.dto.OrderCreateRequest;
import com.mallfei.order.application.service.OrderAdminOperationService;
import com.mallfei.order.application.service.OrderApplicationService;
import com.mallfei.order.application.vo.OrderDetailView;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.domain.model.ProductSnapshot;
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
    private final ProductSnapshotRepository productSnapshotRepository;

    public OrderFacade(OrderDomainService orderDomainService,
                       OrderApplicationService orderApplicationService,
                       OrderAdminOperationService orderAdminOperationService,
                       ProductSnapshotRepository productSnapshotRepository) {
        this.orderDomainService = orderDomainService;
        this.orderApplicationService = orderApplicationService;
        this.orderAdminOperationService = orderAdminOperationService;
        this.productSnapshotRepository = productSnapshotRepository;
    }

    public List<Order> findAll() { return orderDomainService.findAll(); }
    public PageResult<Order> search(String status, String keyword, long page, long size) { return orderDomainService.search(status, keyword, page, size); }
    public PageResult<Order> search(String status, String keyword, long page, long size, String sortBy, String sortOrder) { return orderDomainService.search(status, keyword, page, size, sortBy, sortOrder); }
    public long countAll() { return orderDomainService.countAll(); }
    public long countByStatus(String status) { return orderDomainService.countByStatus(status); }
    public long countCancelled() { return orderDomainService.countCancelled(); }
    public long sumPaidAmount() { return orderDomainService.sumPaidAmount(); }
    public Optional<Order> findByOrderNo(String orderNo) { return orderDomainService.findByOrderNo(orderNo); }
    public Order getByOrderNo(String orderNo) { return orderDomainService.loadOrder(orderNo); }
    public Order getOwnedOrder(Long orderId, Long userId) { return orderDomainService.loadOwnedOrder(orderId, userId); }
    public List<Order> getUserOrders(Long userId) { return orderDomainService.loadUserOrders(userId); }
    public ProductSnapshot getProductSnapshot(Long skuId) { return productSnapshotRepository.findBySkuId(skuId).orElseThrow(() -> BusinessException.badRequest("商品SKU不存在: " + skuId)); }
    public OrderDetailView createOrder(OrderCreateRequest request) { return orderApplicationService.createOrder(request); }
    public OrderDetailView markPaid(String orderNo) { return orderApplicationService.markPaid(orderNo); }
    public void markRefundPending(String orderNo) {
        Order order = orderDomainService.loadOrder(orderNo);
        orderDomainService.update(order.markRefundPending());
    }
    public void ensureRefundable(String orderNo) { orderDomainService.loadOrder(orderNo).ensureRefundable(); }
    public void cancelByAdmin(String orderNo) { orderAdminOperationService.cancel(orderNo); }
    public void shipByAdmin(String orderNo) { orderAdminOperationService.ship(orderNo); }
    public void completeByAdmin(String orderNo) { orderAdminOperationService.complete(orderNo); }
    public void reviseReceiverByAdmin(String orderNo, String receiverName, String receiverPhone, String receiverDetailAddress, String note) { orderAdminOperationService.reviseReceiver(orderNo, receiverName, receiverPhone, receiverDetailAddress, note); }
    public void markPaymentExceptionByAdmin(String orderNo, String note) { orderAdminOperationService.markPaymentException(orderNo, note); }
}
