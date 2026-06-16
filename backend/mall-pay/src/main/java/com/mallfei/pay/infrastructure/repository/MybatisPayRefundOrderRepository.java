package com.mallfei.pay.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallfei.pay.domain.model.PayRefundOrder;
import com.mallfei.pay.domain.repository.PayRefundOrderRepository;
import com.mallfei.pay.infrastructure.persistence.dataobject.PayRefundOrderDO;
import com.mallfei.pay.infrastructure.persistence.mapper.PayRefundOrderMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MybatisPayRefundOrderRepository implements PayRefundOrderRepository {

    private final PayRefundOrderMapper payRefundOrderMapper;

    public MybatisPayRefundOrderRepository(PayRefundOrderMapper payRefundOrderMapper) {
        this.payRefundOrderMapper = payRefundOrderMapper;
    }

    @Override
    public PayRefundOrder save(PayRefundOrder payRefundOrder) {
        PayRefundOrderDO payRefundOrderDO = toDO(payRefundOrder);
        payRefundOrderMapper.insert(payRefundOrderDO);
        return toDomain(payRefundOrderDO);
    }

    @Override
    public void update(PayRefundOrder payRefundOrder) {
        payRefundOrderMapper.updateById(toDO(payRefundOrder));
    }

    @Override
    public Optional<PayRefundOrder> findByRefundNo(String refundNo) {
        PayRefundOrderDO payRefundOrderDO = payRefundOrderMapper.selectOne(new LambdaQueryWrapper<PayRefundOrderDO>()
                .eq(PayRefundOrderDO::getRefundNo, refundNo)
                .last("limit 1"));
        return Optional.ofNullable(payRefundOrderDO).map(this::toDomain);
    }

    private PayRefundOrderDO toDO(PayRefundOrder payRefundOrder) {
        PayRefundOrderDO payRefundOrderDO = new PayRefundOrderDO();
        payRefundOrderDO.setId(payRefundOrder.id());
        payRefundOrderDO.setRefundNo(payRefundOrder.refundNo());
        payRefundOrderDO.setOrderNo(payRefundOrder.orderNo());
        payRefundOrderDO.setPayOrderNo(payRefundOrder.payOrderNo());
        payRefundOrderDO.setUserId(payRefundOrder.userId());
        payRefundOrderDO.setPayChannel(payRefundOrder.payChannel());
        payRefundOrderDO.setRefundAmountCent(payRefundOrder.refundAmountCent());
        payRefundOrderDO.setRefundStatus(payRefundOrder.refundStatus());
        payRefundOrderDO.setTransactionNo(payRefundOrder.transactionNo());
        payRefundOrderDO.setChannelRefundNo(payRefundOrder.channelRefundNo());
        payRefundOrderDO.setRequestPayload(payRefundOrder.requestPayload());
        payRefundOrderDO.setResponsePayload(payRefundOrder.responsePayload());
        payRefundOrderDO.setFailReason(payRefundOrder.failReason());
        payRefundOrderDO.setSuccessAt(payRefundOrder.successAt());
        payRefundOrderDO.setVersion(payRefundOrder.version());
        payRefundOrderDO.setCreatedAt(payRefundOrder.createdAt());
        payRefundOrderDO.setUpdatedAt(payRefundOrder.updatedAt());
        return payRefundOrderDO;
    }

    private PayRefundOrder toDomain(PayRefundOrderDO payRefundOrderDO) {
        return new PayRefundOrder(
                payRefundOrderDO.getId(),
                payRefundOrderDO.getRefundNo(),
                payRefundOrderDO.getOrderNo(),
                payRefundOrderDO.getPayOrderNo(),
                payRefundOrderDO.getUserId(),
                payRefundOrderDO.getPayChannel(),
                payRefundOrderDO.getRefundAmountCent(),
                payRefundOrderDO.getRefundStatus(),
                payRefundOrderDO.getTransactionNo(),
                payRefundOrderDO.getChannelRefundNo(),
                payRefundOrderDO.getRequestPayload(),
                payRefundOrderDO.getResponsePayload(),
                payRefundOrderDO.getFailReason(),
                payRefundOrderDO.getSuccessAt(),
                payRefundOrderDO.getVersion(),
                payRefundOrderDO.getCreatedAt(),
                payRefundOrderDO.getUpdatedAt()
        );
    }
}
