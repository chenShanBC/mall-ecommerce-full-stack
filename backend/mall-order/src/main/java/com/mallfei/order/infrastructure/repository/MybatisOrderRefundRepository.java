package com.mallfei.order.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.domain.model.OrderRefund;
import com.mallfei.order.domain.repository.OrderRefundRepository;
import com.mallfei.order.infrastructure.persistence.dataobject.OrderRefundDO;
import com.mallfei.order.infrastructure.persistence.mapper.OrderRefundMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @Override
    public Optional<OrderRefund> findByRefundNo(String refundNo) {
        OrderRefundDO refundDO = orderRefundMapper.selectOne(new LambdaQueryWrapper<OrderRefundDO>()
                .eq(OrderRefundDO::getRefundNo, refundNo)
                .last("limit 1"));
        return Optional.ofNullable(refundDO).map(this::toDomain);
    }

    @Override
    public List<OrderRefund> findByOrderNo(String orderNo) {
        return orderRefundMapper.selectList(new LambdaQueryWrapper<OrderRefundDO>()
                        .eq(OrderRefundDO::getOrderNo, orderNo)
                        .orderByAsc(OrderRefundDO::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<OrderRefund> search(String status, String keyword) {
        LambdaQueryWrapper<OrderRefundDO> query = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            query.eq(OrderRefundDO::getRefundStatus, status.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(OrderRefundDO::getRefundNo, value)
                    .or()
                    .like(OrderRefundDO::getOrderNo, value));
        }
        query.orderByDesc(OrderRefundDO::getId);
        return orderRefundMapper.selectList(query).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public OrderRefund submitForRefunding(String refundNo) {
        OrderRefund refund = findByRefundNo(refundNo)
                .orElseThrow(() -> BusinessException.badRequest("退款单不存在: " + refundNo));
        OrderRefund refunding = refund.markRefunding();
        return update(refunding);
    }

    private OrderRefundDO toDO(OrderRefund refund) {
        OrderRefundDO refundDO = new OrderRefundDO();
        refundDO.setId(refund.id());
        refundDO.setOrderId(refund.orderId());
        refundDO.setOrderNo(refund.orderNo());
        refundDO.setUserId(refund.userId());
        refundDO.setRefundNo(refund.refundNo());
        refundDO.setRefundAmountCent(refund.refundAmountCent());
        refundDO.setChannelRefundNo(refund.channelRefundNo());
        refundDO.setRefundStatus(refund.refundStatus());
        refundDO.setRefundReason(refund.refundReason());
        refundDO.setFailReason(refund.failReason());
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
                refundDO.getRefundNo(),
                refundDO.getRefundAmountCent(),
                refundDO.getChannelRefundNo(),
                refundDO.getRefundStatus(),
                refundDO.getRefundReason(),
                refundDO.getFailReason(),
                refundDO.getCreatedAt(),
                refundDO.getUpdatedAt()
        );
    }
}
