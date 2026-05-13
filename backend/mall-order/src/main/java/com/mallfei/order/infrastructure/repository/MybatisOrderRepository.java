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

import java.util.List;
import java.util.Optional;

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
        for (OrderItem item : order.items()) {
            OrderItemDO itemDO = toItemDO(item);
            itemDO.setOrderId(orderDO.getId());
            itemDO.setOrderNo(orderDO.getOrderNo());
            orderItemMapper.insert(itemDO);
        }
        return findById(orderDO.getId()).orElseThrow();
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
        return orderMapper.selectList(new LambdaQueryWrapper<OrderDO>().eq(OrderDO::getUserId, userId).orderByDesc(OrderDO::getId)).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Order> findAll() {
        return orderMapper.selectList(new LambdaQueryWrapper<OrderDO>().orderByDesc(OrderDO::getId)).stream().map(this::toDomain).toList();
    }

    @Override
    public PageResult<Order> search(String status, String keyword, long page, long size) {
        return search(status, keyword, page, size, null, null);
    }

    @Override
    public PageResult<Order> search(String status, String keyword, long page, long size, String sortBy, String sortOrder) {
        LambdaQueryWrapper<OrderDO> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) wrapper.eq(OrderDO::getOrderStatus, status);
        if (keyword != null && !keyword.isBlank()) wrapper.and(w -> w.like(OrderDO::getOrderNo, keyword.trim()).or().like(OrderDO::getReceiverName, keyword.trim()).or().like(OrderDO::getReceiverPhone, keyword.trim()));
        applyOrderSort(wrapper, sortBy, sortOrder);
        Page<OrderDO> result = orderMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)), wrapper);
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(), result.getRecords().stream().map(this::toDomain).toList());
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

    private void applyOrderSort(LambdaQueryWrapper<OrderDO> wrapper, String sortBy, String sortOrder) {
        boolean asc = !"desc".equalsIgnoreCase(sortOrder);
        if (sortBy == null || sortBy.isBlank()) {
            wrapper.orderByAsc(OrderDO::getId);
            return;
        }
        switch (sortBy) {
            case "id" -> wrapper.orderBy(true, asc, OrderDO::getId);
            case "orderNo" -> wrapper.orderBy(true, asc, OrderDO::getOrderNo);
            case "status" -> wrapper.orderBy(true, asc, OrderDO::getOrderStatus);
            case "receiverName" -> wrapper.orderBy(true, asc, OrderDO::getReceiverName);
            case "payAmount" -> wrapper.orderBy(true, asc, OrderDO::getPayAmountCent);
            default -> wrapper.orderByAsc(OrderDO::getId);
        }
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
        orderDO.setVersion(order.version());
        return orderDO;
    }

    private OrderItemDO toItemDO(OrderItem item) {
        OrderItemDO itemDO = new OrderItemDO();
        itemDO.setId(item.id());
        itemDO.setOrderId(item.orderId());
        itemDO.setOrderNo(item.orderNo());
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
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemDO>().eq(OrderItemDO::getOrderId, orderDO.getId()).orderByAsc(OrderItemDO::getId)).stream().map(itemDO -> new OrderItem(itemDO.getId(), itemDO.getOrderId(), itemDO.getOrderNo(), itemDO.getSkuId(), itemDO.getSpuId(), itemDO.getSkuName(), itemDO.getSkuImageUrl(), itemDO.getSalePriceCent(), itemDO.getQuantity(), itemDO.getTotalAmountCent())).toList();
        return new Order(orderDO.getId(), orderDO.getOrderNo(), orderDO.getUserId(), orderDO.getOrderStatus(), orderDO.getTotalAmountCent(), orderDO.getPayAmountCent(), orderDO.getFreightAmountCent(), orderDO.getDiscountAmountCent(), orderDO.getReceiverName(), orderDO.getReceiverPhone(), orderDO.getReceiverProvinceName(), orderDO.getReceiverCityName(), orderDO.getReceiverDistrictName(), orderDO.getReceiverDetailAddress(), orderDO.getRemark(), orderDO.getPayType(), orderDO.getPaidAt(), orderDO.getCancelledAt(), orderDO.getShippedAt(), orderDO.getCompletedAt(), orderDO.getVersion(), items);
    }
}
