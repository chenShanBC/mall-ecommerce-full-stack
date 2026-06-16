package com.mallfei.order.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallfei.order.domain.model.OrderRefundItem;
import com.mallfei.order.domain.repository.OrderRefundItemRepository;
import com.mallfei.order.infrastructure.persistence.dataobject.OrderRefundItemDO;
import com.mallfei.order.infrastructure.persistence.mapper.OrderRefundItemMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisOrderRefundItemRepository implements OrderRefundItemRepository {

    private final OrderRefundItemMapper orderRefundItemMapper;

    public MybatisOrderRefundItemRepository(OrderRefundItemMapper orderRefundItemMapper) {
        this.orderRefundItemMapper = orderRefundItemMapper;
    }

    @Override
    public void saveBatch(List<OrderRefundItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (OrderRefundItem item : items) {
            orderRefundItemMapper.insert(toDO(item));
        }
    }

    @Override
    public List<OrderRefundItem> findByRefundNo(String refundNo) {
        return orderRefundItemMapper.selectList(new LambdaQueryWrapper<OrderRefundItemDO>()
                        .eq(OrderRefundItemDO::getRefundNo, refundNo)
                        .orderByAsc(OrderRefundItemDO::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private OrderRefundItemDO toDO(OrderRefundItem item) {
        OrderRefundItemDO itemDO = new OrderRefundItemDO();
        itemDO.setId(item.id());
        itemDO.setRefundId(item.refundId());
        itemDO.setRefundNo(item.refundNo());
        itemDO.setOrderItemId(item.orderItemId());
        itemDO.setSkuId(item.skuId());
        itemDO.setQuantity(item.quantity());
        itemDO.setRefundAmountCent(item.refundAmountCent());
        return itemDO;
    }

    private OrderRefundItem toDomain(OrderRefundItemDO itemDO) {
        return new OrderRefundItem(
                itemDO.getId(),
                itemDO.getRefundId(),
                itemDO.getRefundNo(),
                itemDO.getOrderItemId(),
                itemDO.getSkuId(),
                itemDO.getQuantity(),
                itemDO.getRefundAmountCent()
        );
    }
}
