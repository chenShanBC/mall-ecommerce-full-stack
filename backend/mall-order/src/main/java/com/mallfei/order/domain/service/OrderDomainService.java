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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public PageResult<Order> search(String status, String keyword, java.time.LocalDate startDate, java.time.LocalDate endDate, long page, long size, String sortBy, String sortOrder) { return orderRepository.search(status, keyword, startDate, endDate, page, size, sortBy, sortOrder); }
    public long countAll() { return orderRepository.countAll(); }
    public long countByStatus(String status) { return orderRepository.countByStatus(status); }
    public long countCancelled() { return orderRepository.countCancelled(); }
    public long sumPaidAmount() { return orderRepository.sumPaidAmount(); }
    public List<Order> loadUserOrders(Long userId) { return orderRepository.findByUserId(userId); }
    public PageResult<Order> pageUserOrders(Long userId, String status, String keyword, LocalDateTime createdAfter, long page, long size) { return orderRepository.pageByUserId(userId, status, keyword, createdAfter, page, size); }

    public Order loadOrder(Long orderId) { return orderRepository.findById(orderId).orElseThrow(() -> BusinessException.badRequest("订单不存在")); }
    public Order loadOrder(String orderNo) { return orderRepository.findByOrderNo(orderNo).orElseThrow(() -> BusinessException.badRequest("订单不存在")); }
    public Optional<Order> findByOrderNo(String orderNo) { return orderRepository.findByOrderNo(orderNo); }
    public Order save(Order order) { return orderRepository.save(order); }
    public void update(Order order) { orderRepository.update(order); }
    public void reviseReceiver(Order order) { if (!orderRepository.reviseReceiver(order)) throw BusinessException.badRequest("当前订单状态不允许修改收货信息"); }
    public void updateReceiverAddress(String orderNo, String receiverName, String receiverPhone, String receiverProvinceName, String receiverCityName, String receiverDistrictName, String receiverDetailAddress, String note) { if (!orderRepository.updateReceiverAddress(orderNo, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, note)) throw BusinessException.badRequest("当前订单状态不允许修改收货信息"); }
    public int markPaymentException(String orderNo, String note) {
        int affectedRows = orderRepository.markPaymentException(orderNo, note);
        if (affectedRows <= 0) {
            Order current = loadOrder(orderNo);
            throw BusinessException.badRequest("仅待支付、已支付、处理中或已发货订单可标记支付异常，当前状态=" + current.orderStatus());
        }
        return affectedRows;
    }
    public void restorePendingPayment(String orderNo, String note) { if (!orderRepository.restorePendingPayment(orderNo, note)) throw BusinessException.badRequest("仅支付异常订单可恢复待支付"); }
    public boolean markPaid(String orderNo, LocalDateTime paidAt) { return orderRepository.markPaid(orderNo, paidAt); }
    public void markPaidByAdmin(String orderNo, LocalDateTime paidAt, String note) { if (!orderRepository.markPaidByAdmin(orderNo, paidAt, note)) throw BusinessException.badRequest("仅待支付或支付异常订单可人工确认已支付"); }
    public boolean closeTimedOut(String orderNo, LocalDateTime now) { return orderRepository.closeTimedOut(orderNo, now); }
    public List<Order> findTimedOutPendingOrders(LocalDateTime now, int limit) { return orderRepository.findTimedOutPendingOrders(now, limit); }
    public List<Order> findTimedOutPendingOrdersByUserId(Long userId, LocalDateTime now, int limit) { return orderRepository.findTimedOutPendingOrdersByUserId(userId, now, limit); }
    public void markUserDeleted(Long orderId, Long userId) { orderRepository.markUserDeleted(orderId, userId); }
    public Order loadOwnedOrder(Long orderId, Long userId) { Order order = loadOrder(orderId); ensureOwnedBy(order, userId); return order; }

    public Order createPendingOrder(String orderNo, Long userId, OrderCreateRequest request) {
        return createPendingOrder(orderNo, userId, request, LocalDateTime.now().plusMinutes(2));
    }

    public Order createPendingOrder(String orderNo, Long userId, OrderCreateRequest request, LocalDateTime expireTime) {
        Map<Long, Integer> skuQuantityMap = request.items().stream()
                .collect(Collectors.toMap(OrderCreateRequest.Item::skuId, OrderCreateRequest.Item::quantity, Integer::sum));
        Map<Long, ProductSnapshot> snapshotMap = productSnapshotRepository.findBySkuIds(List.copyOf(skuQuantityMap.keySet()))
                .stream()
                .collect(Collectors.toMap(ProductSnapshot::skuId, Function.identity()));
        List<OrderItem> items = skuQuantityMap.entrySet().stream().map(entry -> {
            ProductSnapshot snapshot = Optional.ofNullable(snapshotMap.get(entry.getKey()))
                    .orElseThrow(() -> BusinessException.badRequest("商品SKU不存在: " + entry.getKey()));
            long totalAmount = snapshot.salePriceCent() * entry.getValue();
            return new OrderItem(null, null, orderNo, snapshot.skuId(), snapshot.spuId(), snapshot.skuName(), snapshot.skuImageUrl(), snapshot.salePriceCent(), entry.getValue(), totalAmount);
        }).toList();
        long totalAmount = items.stream().mapToLong(OrderItem::totalAmountCent).sum();
        return Order.createPending(orderNo, userId, totalAmount, request.receiverName(), request.receiverPhone(), request.receiverProvinceName(), request.receiverCityName(), request.receiverDistrictName(), request.receiverDetailAddress(), request.remark(), expireTime, items);
    }

    public Order cancelTimedOutIfNecessary(Order order, LocalDateTime now, long timeoutMinutes) { return order.cancelIfTimedOut(now, timeoutMinutes); }
    public List<Order> locateTimedOutOrders(Long userId, LocalDateTime now, long timeoutMinutes) { return loadUserOrders(userId).stream().filter(Order::pendingPayment).map(order -> order.cancelIfTimedOut(now, timeoutMinutes)).filter(Order::timeoutCancelled).toList(); }
    public List<Order> locateAllTimedOutOrders(LocalDateTime now, long timeoutMinutes) { return findAll().stream().filter(Order::pendingPayment).map(order -> order.cancelIfTimedOut(now, timeoutMinutes)).filter(Order::timeoutCancelled).toList(); }
    public void ensureOwnedBy(Order order, Long userId) { if (!order.belongsTo(userId)) throw BusinessException.forbidden("无权访问当前订单"); }
}
