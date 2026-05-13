package com.mallfei.order.domain.service;

import com.mallfei.common.api.PageResult;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.application.dto.OrderCreateRequest;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.domain.model.OrderItem;
import com.mallfei.order.domain.model.ProductSnapshot;
import com.mallfei.order.domain.repository.OrderRepository;
import com.mallfei.order.domain.repository.ProductSnapshotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderDomainService {

    private final OrderRepository orderRepository;
    private final ProductSnapshotRepository productSnapshotRepository;

    public OrderDomainService(OrderRepository orderRepository,
                              ProductSnapshotRepository productSnapshotRepository) {
        this.orderRepository = orderRepository;
        this.productSnapshotRepository = productSnapshotRepository;
    }

    public List<Order> findAll() { return orderRepository.findAll(); }
    public PageResult<Order> search(String status, String keyword, long page, long size) { return orderRepository.search(status, keyword, page, size); }
    public PageResult<Order> search(String status, String keyword, long page, long size, String sortBy, String sortOrder) { return orderRepository.search(status, keyword, page, size, sortBy, sortOrder); }
    public long countAll() { return orderRepository.countAll(); }
    public long countByStatus(String status) { return orderRepository.countByStatus(status); }
    public long countCancelled() { return orderRepository.countCancelled(); }
    public long sumPaidAmount() { return orderRepository.sumPaidAmount(); }
    public List<Order> loadUserOrders(Long userId) { return orderRepository.findByUserId(userId); }

    public Order loadOrder(Long orderId) { return orderRepository.findById(orderId).orElseThrow(() -> BusinessException.badRequest("订单不存在")); }
    public Order loadOrder(String orderNo) { return orderRepository.findByOrderNo(orderNo).orElseThrow(() -> BusinessException.badRequest("订单不存在")); }
    public Optional<Order> findByOrderNo(String orderNo) { return orderRepository.findByOrderNo(orderNo); }
    public Order save(Order order) { return orderRepository.save(order); }
    public void update(Order order) { orderRepository.update(order); }
    public Order loadOwnedOrder(Long orderId, Long userId) { Order order = loadOrder(orderId); ensureOwnedBy(order, userId); return order; }

    public Order createPendingOrder(String orderNo, Long userId, OrderCreateRequest request) {
        List<ProductSnapshot> snapshots = request.items().stream().map(item -> productSnapshotRepository.findBySkuId(item.skuId()).orElseThrow(() -> BusinessException.badRequest("商品SKU不存在: " + item.skuId()))).toList();
        List<OrderItem> items = request.items().stream().map(item -> {
            ProductSnapshot snapshot = snapshots.stream().filter(candidate -> candidate.skuId().equals(item.skuId())).findFirst().orElseThrow(() -> BusinessException.badRequest("商品SKU不存在: " + item.skuId()));
            long totalAmount = snapshot.salePriceCent() * item.quantity();
            return new OrderItem(null, null, orderNo, snapshot.skuId(), snapshot.spuId(), snapshot.skuName(), snapshot.skuImageUrl(), snapshot.salePriceCent(), item.quantity(), totalAmount);
        }).toList();
        long totalAmount = items.stream().mapToLong(OrderItem::totalAmountCent).sum();
        return Order.createPending(orderNo, userId, totalAmount, request.receiverName(), request.receiverPhone(), request.receiverProvinceName(), request.receiverCityName(), request.receiverDistrictName(), request.receiverDetailAddress(), request.remark(), items);
    }

    public Order cancelTimedOutIfNecessary(Order order, LocalDateTime now, long timeoutMinutes) { return order.cancelIfTimedOut(now, timeoutMinutes); }
    public List<Order> locateTimedOutOrders(Long userId, LocalDateTime now, long timeoutMinutes) { return loadUserOrders(userId).stream().filter(Order::pendingPayment).map(order -> order.cancelIfTimedOut(now, timeoutMinutes)).filter(Order::timeoutCancelled).toList(); }
    public List<Order> locateAllTimedOutOrders(LocalDateTime now, long timeoutMinutes) { return findAll().stream().filter(Order::pendingPayment).map(order -> order.cancelIfTimedOut(now, timeoutMinutes)).filter(Order::timeoutCancelled).toList(); }
    public void ensureOwnedBy(Order order, Long userId) { if (!order.belongsTo(userId)) throw BusinessException.forbidden("无权访问当前订单"); }
}
