package com.mallfei.aftersale.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallfei.aftersale.domain.model.AftersaleOrder;
import com.mallfei.aftersale.domain.repository.AftersaleOrderRepository;
import com.mallfei.aftersale.infrastructure.persistence.dataobject.AftersaleOrderDO;
import com.mallfei.aftersale.infrastructure.persistence.mapper.AftersaleOrderMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisAftersaleOrderRepository implements AftersaleOrderRepository {

    private final AftersaleOrderMapper aftersaleOrderMapper;

    public MybatisAftersaleOrderRepository(AftersaleOrderMapper aftersaleOrderMapper) {
        this.aftersaleOrderMapper = aftersaleOrderMapper;
    }

    @Override
    public AftersaleOrder save(AftersaleOrder aftersaleOrder) {
        AftersaleOrderDO entity = toDO(aftersaleOrder);
        aftersaleOrderMapper.insert(entity);
        return toDomain(aftersaleOrderMapper.selectById(entity.getId()));
    }

    @Override
    public Optional<AftersaleOrder> findByAftersaleNo(String aftersaleNo) {
        AftersaleOrderDO entity = aftersaleOrderMapper.selectOne(new LambdaQueryWrapper<AftersaleOrderDO>()
                .eq(AftersaleOrderDO::getAftersaleNo, aftersaleNo)
                .last("limit 1"));
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public Optional<AftersaleOrder> findLatestByOrderNo(String orderNo) {
        AftersaleOrderDO entity = aftersaleOrderMapper.selectOne(new LambdaQueryWrapper<AftersaleOrderDO>()
                .eq(AftersaleOrderDO::getOrderNo, orderNo)
                .orderByDesc(AftersaleOrderDO::getId)
                .last("limit 1"));
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public List<AftersaleOrder> findAll() {
        return aftersaleOrderMapper.selectList(new LambdaQueryWrapper<AftersaleOrderDO>()
                        .orderByDesc(AftersaleOrderDO::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void update(AftersaleOrder aftersaleOrder) {
        aftersaleOrderMapper.updateById(toDO(aftersaleOrder));
    }

    private AftersaleOrderDO toDO(AftersaleOrder order) {
        AftersaleOrderDO entity = new AftersaleOrderDO();
        entity.setId(order.id());
        entity.setAftersaleNo(order.aftersaleNo());
        entity.setOrderNo(order.orderNo());
        entity.setUserId(order.userId());
        entity.setAftersaleType(order.aftersaleType());
        entity.setStatus(order.status());
        entity.setOriginOrderStatus(order.originOrderStatus());
        entity.setRefundAmountCent(order.refundAmountCent());
        entity.setReason(order.reason());
        entity.setRejectReason(order.rejectReason());
        entity.setRefundNo(order.refundNo());
        entity.setFailReason(order.failReason());
        entity.setVersion(order.version());
        entity.setReviewedAt(order.reviewedAt());
        entity.setCreatedAt(order.createdAt());
        entity.setUpdatedAt(order.updatedAt());
        return entity;
    }

    private AftersaleOrder toDomain(AftersaleOrderDO entity) {
        return new AftersaleOrder(
                entity.getId(),
                entity.getAftersaleNo(),
                entity.getOrderNo(),
                entity.getUserId(),
                entity.getAftersaleType(),
                entity.getStatus(),
                entity.getOriginOrderStatus(),
                entity.getRefundAmountCent(),
                entity.getReason(),
                entity.getRejectReason(),
                entity.getRefundNo(),
                entity.getFailReason(),
                entity.getVersion(),
                entity.getReviewedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
