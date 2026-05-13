package com.mallfei.order.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallfei.order.domain.model.OrderRefund;
import com.mallfei.order.domain.repository.OrderRefundRepository;
import com.mallfei.order.infrastructure.persistence.dataobject.OrderRefundDO;
import com.mallfei.order.infrastructure.persistence.mapper.OrderRefundMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MybatisOrderRefundRepository implements OrderRefundRepository {

    private final OrderRefundMapper orderRefundMapper;

    public MybatisOrderRefundRepository(OrderRefundMapper orderRefundMapper) {
        this.orderRefundMapper = orderRefundMapper;
    }

    @Override
    public OrderRefund save(OrderRefund refund) {
        OrderRefundDO refundDO = toDO(refund);
        orderRefundMapper.insert(refundDO);
        return toDomain(orderRefundMapper.selectById(refundDO.getId()));
    }

    @Override
    public OrderRefund update(OrderRefund refund) {
        OrderRefundDO refundDO = toDO(refund);
        orderRefundMapper.updateById(refundDO);
        return toDomain(orderRefundMapper.selectById(refundDO.getId()));
    }

    @Override
    public Optional<OrderRefund> findLatestByOrderNo(String orderNo) {
        OrderRefundDO refundDO = orderRefundMapper.selectOne(new LambdaQueryWrapper<OrderRefundDO>()
                .eq(OrderRefundDO::getOrderNo, orderNo)
                .orderByDesc(OrderRefundDO::getId)
                .last("limit 1"));
        return Optional.ofNullable(refundDO).map(this::toDomain);
    }

    private OrderRefundDO toDO(OrderRefund refund) {
        OrderRefundDO refundDO = new OrderRefundDO();
        refundDO.setId(refund.id());
        refundDO.setOrderId(refund.orderId());
        refundDO.setOrderNo(refund.orderNo());
        refundDO.setUserId(refund.userId());
        refundDO.setRefundStatus(refund.refundStatus());
        refundDO.setRefundReason(refund.refundReason());
        refundDO.setCreatedAt(refund.createdAt());
        refundDO.setUpdatedAt(refund.updatedAt());
        return refundDO;
    }

    private OrderRefund toDomain(OrderRefundDO refundDO) {
        return new OrderRefund(
                refundDO.getId(),
                refundDO.getOrderId(),
                refundDO.getOrderNo(),
                refundDO.getUserId(),
                refundDO.getRefundStatus(),
                refundDO.getRefundReason(),
                refundDO.getCreatedAt(),
                refundDO.getUpdatedAt()
        );
    }
}
