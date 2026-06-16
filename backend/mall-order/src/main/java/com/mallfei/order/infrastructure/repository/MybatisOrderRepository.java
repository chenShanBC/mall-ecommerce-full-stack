package com.mallfei.order.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallfei.common.api.PageResult;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.domain.model.OrderItem;
import com.mallfei.order.domain.repository.OrderRepository;
import com.mallfei.order.infrastructure.persistence.dataobject.OrderDO;
import com.mallfei.order.infrastructure.persistence.dataobject.OrderItemDO;
import com.mallfei.order.infrastructure.persistence.mapper.OrderItemMapper;
import com.mallfei.order.infrastructure.persistence.mapper.OrderMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class MybatisOrderRepository implements OrderRepository {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    public MybatisOrderRepository(OrderMapper orderMapper, OrderItemMapper orderItemMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    public Order save(Order order) {
        OrderDO orderDO = toOrderDO(order);
        orderMapper.insert(orderDO);
        List<OrderItemDO> itemDOs = order.items().stream()
                .map(item -> toItemDO(item, orderDO.getId(), orderDO.getOrderNo()))
                .toList();
        if (!itemDOs.isEmpty()) {
            orderItemMapper.insertBatch(itemDOs);
        }
        return toDomain(orderDO, itemDOs);
    }

    @Override
    public Optional<Order> findById(Long id) {
        OrderDO orderDO = orderMapper.selectById(id);
        return Optional.ofNullable(orderDO).map(this::toDomain);
    }

    @Override
    public Optional<Order> findByOrderNo(String orderNo) {
        OrderDO orderDO = orderMapper.selectOne(new LambdaQueryWrapper<OrderDO>().eq(OrderDO::getOrderNo, orderNo).last("limit 1"));
        return Optional.ofNullable(orderDO).map(this::toDomain);
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        return toDomains(orderMapper.selectList(new LambdaQueryWrapper<OrderDO>()
                .eq(OrderDO::getUserId, userId)
                .and(wrapper -> wrapper.isNull(OrderDO::getUserDeleted).or().eq(OrderDO::getUserDeleted, 0))
                .orderByDesc(OrderDO::getId)));
    }

    @Override
    public PageResult<Order> pageByUserId(Long userId, String status, String keyword, LocalDateTime createdAfter, long page, long size) {
        LambdaQueryWrapper<OrderDO> wrapper = new LambdaQueryWrapper<OrderDO>()
                .eq(OrderDO::getUserId, userId)
                .and(w -> w.isNull(OrderDO::getUserDeleted).or().eq(OrderDO::getUserDeleted, 0));
        if (createdAfter != null) {
            String minVisibleOrderNo = buildMinOrderNo(createdAfter);
            wrapper.and(w -> w.ne(OrderDO::getOrderStatus, Order.STATUS_COMPLETED)
                    .or()
                    .ge(OrderDO::getOrderNo, minVisibleOrderNo));
        }
        applyUserOrderStatusFilter(wrapper, status);
        if (keyword != null && !keyword.isBlank()) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(w -> w.like(OrderDO::getOrderNo, trimmedKeyword)
                    .or().like(OrderDO::getReceiverName, trimmedKeyword)
                    .or().like(OrderDO::getReceiverPhone, trimmedKeyword));
        }
        wrapper.orderByDesc(OrderDO::getId);
        Page<OrderDO> result = orderMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)), wrapper);
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(), toDomains(result.getRecords()));
    }

    @Override
    public List<Order> findAll() {
        return toDomains(orderMapper.selectList(new LambdaQueryWrapper<OrderDO>().orderByDesc(OrderDO::getId)));
    }

    @Override
    public PageResult<Order> search(String status, String keyword, long page, long size) {
        return search(status, keyword, page, size, null, null);
    }

    @Override
    public PageResult<Order> search(String status, String keyword, long page, long size, String sortBy, String sortOrder) {
        return search(status, keyword, null, null, page, size, sortBy, sortOrder);
    }

    @Override
    public PageResult<Order> search(String status, String keyword, LocalDate startDate, LocalDate endDate, long page, long size, String sortBy, String sortOrder) {
        LambdaQueryWrapper<OrderDO> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) wrapper.eq(OrderDO::getOrderStatus, status);
        if (keyword != null && !keyword.isBlank()) wrapper.and(w -> w.like(OrderDO::getOrderNo, keyword.trim()).or().like(OrderDO::getReceiverName, keyword.trim()).or().like(OrderDO::getReceiverPhone, keyword.trim()));
        applyOrderDateRange(wrapper, startDate, endDate);
        applyOrderSort(wrapper, sortBy, sortOrder);
        Page<OrderDO> result = orderMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)), wrapper);
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(), toDomains(result.getRecords()));
    }

    @Override
    public long countAll() { return orderMapper.selectCount(null); }

    @Override
    public long countByStatus(String status) { return orderMapper.selectCount(new LambdaQueryWrapper<OrderDO>().eq(OrderDO::getOrderStatus, status)); }

    @Override
    public long countCancelled() {
        return orderMapper.selectCount(new LambdaQueryWrapper<OrderDO>().in(OrderDO::getOrderStatus, List.of(Order.STATUS_CANCELLED, Order.STATUS_TIMEOUT_CANCELLED, Order.STATUS_CLOSED, Order.STATUS_REFUNDED)));
    }

    @Override
    public long sumPaidAmount() {
        return orderMapper.selectList(new LambdaQueryWrapper<OrderDO>().in(OrderDO::getOrderStatus, List.of(Order.STATUS_PAID, Order.STATUS_PROCESSING, Order.STATUS_SHIPPED, Order.STATUS_COMPLETED, Order.STATUS_REFUND_PENDING, Order.STATUS_REFUNDED, Order.STATUS_PARTIALLY_REFUNDED)))
                .stream().mapToLong(order -> order.getPayAmountCent() == null ? 0L : order.getPayAmountCent()).sum();
    }

    @Override
    public void update(Order order) { orderMapper.updateById(toOrderDO(order)); }

    @Override
    public boolean reviseReceiver(Order order) {
        return orderMapper.reviseReceiver(order.orderNo(), order.receiverName(), order.receiverPhone(), order.receiverProvinceName(), order.receiverCityName(), order.receiverDistrictName(), order.receiverDetailAddress(), order.remark()) > 0;
    }

    @Override
    public boolean updateReceiverAddress(String orderNo, String receiverName, String receiverPhone, String receiverProvinceName, String receiverCityName, String receiverDistrictName, String receiverDetailAddress, String note) {
        return orderMapper.updateReceiverAddress(orderNo, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, note) > 0;
    }

    @Override
    public int markPaymentException(String orderNo, String note) {
        return orderMapper.markPaymentException(orderNo, normalizeNote(note));
    }

    @Override
    public boolean restorePendingPayment(String orderNo, String note) {
        return orderMapper.restorePendingPayment(orderNo, normalizeNote(note)) > 0;
    }

    @Override
    public boolean markPaid(String orderNo, LocalDateTime paidAt) {
        return orderMapper.markPaid(orderNo, paidAt) > 0;
    }

    @Override
    public boolean markPaidByAdmin(String orderNo, LocalDateTime paidAt, String note) {
        return orderMapper.markPaidByAdmin(orderNo, paidAt, normalizeNote(note)) > 0;
    }

    @Override
    public void replaceOrderItem(Order order, OrderItemReplacement replacement) {
        OrderItemDO itemDO = orderItemMapper.selectById(replacement.orderItemId());
        if (itemDO == null || !order.id().equals(itemDO.getOrderId())) {
            throw com.mallfei.common.exception.BusinessException.badRequest("订单商品项不存在");
        }
        Long originTotalAmountCent = itemDO.getTotalAmountCent();
        Long targetTotalAmountCent = replacement.targetSalePriceCent() * itemDO.getQuantity();
        itemDO.setSkuId(replacement.targetSkuId());
        itemDO.setSpuId(replacement.targetSpuId());
        itemDO.setSkuName(replacement.targetSkuName());
        itemDO.setSkuImageUrl(replacement.targetSkuImageUrl());
        itemDO.setSalePriceCent(replacement.targetSalePriceCent());
        itemDO.setTotalAmountCent(targetTotalAmountCent);
        orderItemMapper.updateById(itemDO);

        OrderDO orderDO = toOrderDO(order.appendAdminRemark(replacement.remark()));
        orderDO.setTotalAmountCent(order.totalAmountCent() - originTotalAmountCent + targetTotalAmountCent);
        orderDO.setPayAmountCent(order.payAmountCent() - originTotalAmountCent + targetTotalAmountCent);
        orderMapper.updateById(orderDO);
    }

    @Override
    public boolean closeTimedOut(String orderNo, LocalDateTime now) {
        return orderMapper.closeTimedOut(orderNo, now) > 0;
    }

    @Override
    public List<Order> findTimedOutPendingOrders(LocalDateTime now, int limit) {
        return toDomains(orderMapper.selectList(new LambdaQueryWrapper<OrderDO>()
                        .eq(OrderDO::getOrderStatus, Order.STATUS_PENDING_PAYMENT)
                        .le(OrderDO::getExpireTime, now)
                        .orderByAsc(OrderDO::getExpireTime)
                        .last("limit " + Math.max(1, limit))));
    }

    @Override
    public List<Order> findTimedOutPendingOrdersByUserId(Long userId, LocalDateTime now, int limit) {
        return toDomains(orderMapper.selectList(new LambdaQueryWrapper<OrderDO>()
                        .eq(OrderDO::getUserId, userId)
                        .eq(OrderDO::getOrderStatus, Order.STATUS_PENDING_PAYMENT)
                        .le(OrderDO::getExpireTime, now)
                        .and(wrapper -> wrapper.isNull(OrderDO::getUserDeleted).or().eq(OrderDO::getUserDeleted, 0))
                        .orderByAsc(OrderDO::getExpireTime)
                        .last("limit " + Math.max(1, limit))));
    }

    @Override
    public void markUserDeleted(Long orderId, Long userId) {
        OrderDO orderDO = new OrderDO();
        orderDO.setUserDeleted(1);
        orderDO.setUserDeletedAt(LocalDateTime.now());
        orderMapper.update(orderDO, new LambdaQueryWrapper<OrderDO>()
                .eq(OrderDO::getId, orderId)
                .eq(OrderDO::getUserId, userId));
    }

    private void applyUserOrderStatusFilter(LambdaQueryWrapper<OrderDO> wrapper, String status) {
        String normalizedStatus = status == null || status.isBlank() ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalizedStatus)) {
            return;
        }
        if ("PENDING_PAYMENT_GROUP".equals(normalizedStatus) || Order.STATUS_PENDING_PAYMENT.equals(normalizedStatus)) {
            wrapper.eq(OrderDO::getOrderStatus, Order.STATUS_PENDING_PAYMENT);
            return;
        }
        if ("PAID_GROUP".equals(normalizedStatus) || Order.STATUS_PAID.equals(normalizedStatus)) {
            wrapper.eq(OrderDO::getOrderStatus, Order.STATUS_PAID);
            return;
        }
        if ("PROCESSING_GROUP".equals(normalizedStatus) || Order.STATUS_PROCESSING.equals(normalizedStatus)) {
            wrapper.in(OrderDO::getOrderStatus, List.of(Order.STATUS_PROCESSING, Order.STATUS_REFUND_PENDING, Order.STATUS_PAYMENT_EXCEPTION));
            return;
        }
        if ("SHIPPED".equals(normalizedStatus) || "SHIPPED_GROUP".equals(normalizedStatus) || "DELIVERED".equals(normalizedStatus)) {
            wrapper.in(OrderDO::getOrderStatus, List.of(Order.STATUS_SHIPPED));
            return;
        }
        if ("COMPLETED_GROUP".equals(normalizedStatus) || Order.STATUS_COMPLETED.equals(normalizedStatus)) {
            wrapper.eq(OrderDO::getOrderStatus, Order.STATUS_COMPLETED);
            return;
        }
        if ("REFUNDED_GROUP".equals(normalizedStatus) || Order.STATUS_REFUNDED.equals(normalizedStatus)) {
            wrapper.in(OrderDO::getOrderStatus, List.of(Order.STATUS_REFUNDED, Order.STATUS_PARTIALLY_REFUNDED));
            return;
        }
        if ("CANCELLED_GROUP".equals(normalizedStatus)) {
            wrapper.in(OrderDO::getOrderStatus, List.of(Order.STATUS_CANCELLED, Order.STATUS_TIMEOUT_CANCELLED, Order.STATUS_CLOSED, Order.STATUS_REFUND_CLOSED));
            return;
        }
        wrapper.eq(OrderDO::getOrderStatus, normalizedStatus);
    }

    private void applyOrderDateRange(LambdaQueryWrapper<OrderDO> wrapper, LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return;
        }
        if (startDate != null) {
            wrapper.ge(OrderDO::getCreatedAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.lt(OrderDO::getCreatedAt, endDate.plusDays(1).atStartOfDay());
        }
    }

    private void applyOrderSort(LambdaQueryWrapper<OrderDO> wrapper, String sortBy, String sortOrder) {
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if (sortBy == null || sortBy.isBlank()) {
            wrapper.orderByDesc(OrderDO::getId);
            return;
        }
        switch (sortBy) {
            case "id" -> wrapper.orderBy(true, asc, OrderDO::getId);
            case "orderNo" -> wrapper.orderBy(true, asc, OrderDO::getOrderNo);
            case "status" -> wrapper.orderBy(true, asc, OrderDO::getOrderStatus);
            case "receiverName" -> wrapper.orderBy(true, asc, OrderDO::getReceiverName);
            case "payAmount" -> wrapper.orderBy(true, asc, OrderDO::getPayAmountCent);
            default -> wrapper.orderByDesc(OrderDO::getId);
        }
    }

    private String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    private String buildMinOrderNo(LocalDateTime createdAfter) {
        long timestamp = createdAfter.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        return "ORD" + timestamp;
    }

    private OrderDO toOrderDO(Order order) {
        OrderDO orderDO = new OrderDO();
        orderDO.setId(order.id());
        orderDO.setOrderNo(order.orderNo());
        orderDO.setUserId(order.userId());
        orderDO.setOrderStatus(order.orderStatus());
        orderDO.setTotalAmountCent(order.totalAmountCent());
        orderDO.setPayAmountCent(order.payAmountCent());
        orderDO.setFreightAmountCent(order.freightAmountCent());
        orderDO.setDiscountAmountCent(order.discountAmountCent());
        orderDO.setReceiverName(order.receiverName());
        orderDO.setReceiverPhone(order.receiverPhone());
        orderDO.setReceiverProvinceName(order.receiverProvinceName());
        orderDO.setReceiverCityName(order.receiverCityName());
        orderDO.setReceiverDistrictName(order.receiverDistrictName());
        orderDO.setReceiverDetailAddress(order.receiverDetailAddress());
        orderDO.setRemark(order.remark());
        orderDO.setPayType(order.payType());
        orderDO.setPaidAt(order.paidAt());
        orderDO.setCancelledAt(order.cancelledAt());
        orderDO.setShippedAt(order.shippedAt());
        orderDO.setCompletedAt(order.completedAt());
        orderDO.setExpireTime(order.expireTime());
        orderDO.setCreatedAt(order.createdAt());
        orderDO.setVersion(order.version());
        return orderDO;
    }

    private OrderItemDO toItemDO(OrderItem item) {
        return toItemDO(item, item.orderId(), item.orderNo());
    }

    private OrderItemDO toItemDO(OrderItem item, Long orderId, String orderNo) {
        OrderItemDO itemDO = new OrderItemDO();
        itemDO.setId(item.id());
        itemDO.setOrderId(orderId);
        itemDO.setOrderNo(orderNo);
        itemDO.setSkuId(item.skuId());
        itemDO.setSpuId(item.spuId());
        itemDO.setSkuName(item.skuName());
        itemDO.setSkuImageUrl(item.skuImageUrl());
        itemDO.setSalePriceCent(item.salePriceCent());
        itemDO.setQuantity(item.quantity());
        itemDO.setTotalAmountCent(item.totalAmountCent());
        return itemDO;
    }

    private Order toDomain(OrderDO orderDO) {
        List<OrderItemDO> itemDOs = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemDO>()
                .eq(OrderItemDO::getOrderId, orderDO.getId())
                .orderByAsc(OrderItemDO::getId));
        return toDomain(orderDO, itemDOs);
    }

    private List<Order> toDomains(List<OrderDO> orderDOs) {
        if (orderDOs == null || orderDOs.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orderDOs.stream().map(OrderDO::getId).toList();
        Map<Long, List<OrderItemDO>> itemsByOrderId = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemDO>()
                        .in(OrderItemDO::getOrderId, orderIds)
                        .orderByAsc(OrderItemDO::getOrderId)
                        .orderByAsc(OrderItemDO::getId))
                .stream()
                .collect(Collectors.groupingBy(OrderItemDO::getOrderId, Collectors.toList()));
        return orderDOs.stream()
                .map(orderDO -> toDomain(orderDO, itemsByOrderId.getOrDefault(orderDO.getId(), Collections.emptyList())))
                .toList();
    }

    private Order toDomain(OrderDO orderDO, List<OrderItemDO> itemDOs) {
        List<OrderItem> items = itemDOs.stream().map(this::toOrderItem).toList();
        return new Order(orderDO.getId(), orderDO.getOrderNo(), orderDO.getUserId(), orderDO.getOrderStatus(), orderDO.getTotalAmountCent(), orderDO.getPayAmountCent(), orderDO.getFreightAmountCent(), orderDO.getDiscountAmountCent(), orderDO.getReceiverName(), orderDO.getReceiverPhone(), orderDO.getReceiverProvinceName(), orderDO.getReceiverCityName(), orderDO.getReceiverDistrictName(), orderDO.getReceiverDetailAddress(), orderDO.getRemark(), orderDO.getPayType(), orderDO.getPaidAt(), orderDO.getCancelledAt(), orderDO.getShippedAt(), orderDO.getCompletedAt(), orderDO.getExpireTime(), orderDO.getCreatedAt(), orderDO.getVersion(), items);
    }

    private OrderItem toOrderItem(OrderItemDO itemDO) {
        return new OrderItem(itemDO.getId(), itemDO.getOrderId(), itemDO.getOrderNo(), itemDO.getSkuId(), itemDO.getSpuId(), itemDO.getSkuName(), itemDO.getSkuImageUrl(), itemDO.getSalePriceCent(), itemDO.getQuantity(), itemDO.getTotalAmountCent());
    }
}
