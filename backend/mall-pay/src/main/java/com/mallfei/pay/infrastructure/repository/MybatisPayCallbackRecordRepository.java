package com.mallfei.pay.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallfei.common.api.PageResult;
import com.mallfei.pay.domain.model.PayCallbackRecord;
import com.mallfei.pay.domain.repository.PayCallbackRecordRepository;
import com.mallfei.pay.infrastructure.persistence.dataobject.PayCallbackRecordDO;
import com.mallfei.pay.infrastructure.persistence.mapper.PayCallbackRecordMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisPayCallbackRecordRepository implements PayCallbackRecordRepository {

    private final PayCallbackRecordMapper payCallbackRecordMapper;

    public MybatisPayCallbackRecordRepository(PayCallbackRecordMapper payCallbackRecordMapper) {
        this.payCallbackRecordMapper = payCallbackRecordMapper;
    }

    @Override
    public PayCallbackRecord save(PayCallbackRecord payCallbackRecord) {
        PayCallbackRecordDO payCallbackRecordDO = toDO(payCallbackRecord);
        payCallbackRecordMapper.insert(payCallbackRecordDO);
        return toDomain(payCallbackRecordDO);
    }

    @Override
    public Optional<PayCallbackRecord> findLatestByTransactionNo(String transactionNo) {
        PayCallbackRecordDO payCallbackRecordDO = payCallbackRecordMapper.selectOne(new LambdaQueryWrapper<PayCallbackRecordDO>()
                .eq(PayCallbackRecordDO::getTransactionNo, transactionNo)
                .orderByDesc(PayCallbackRecordDO::getId)
                .last("limit 1"));
        return Optional.ofNullable(payCallbackRecordDO).map(this::toDomain);
    }

    @Override
    public Optional<PayCallbackRecord> findLatestByOutTradeNo(String outTradeNo) {
        PayCallbackRecordDO payCallbackRecordDO = payCallbackRecordMapper.selectOne(new LambdaQueryWrapper<PayCallbackRecordDO>()
                .eq(PayCallbackRecordDO::getOutTradeNo, outTradeNo)
                .orderByDesc(PayCallbackRecordDO::getId)
                .last("limit 1"));
        return Optional.ofNullable(payCallbackRecordDO).map(this::toDomain);
    }

    @Override
    public PageResult<PayCallbackRecord> search(String processStatus, String keyword, long page, long size) {
        LambdaQueryWrapper<PayCallbackRecordDO> wrapper = new LambdaQueryWrapper<>();
        if (processStatus != null && !processStatus.isBlank()) {
            wrapper.eq(PayCallbackRecordDO::getProcessStatus, processStatus.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = keyword.trim();
            wrapper.and(query -> query.like(PayCallbackRecordDO::getOrderNo, like)
                    .or().like(PayCallbackRecordDO::getPayOrderNo, like)
                    .or().like(PayCallbackRecordDO::getRefundNo, like)
                    .or().like(PayCallbackRecordDO::getTransactionNo, like)
                    .or().like(PayCallbackRecordDO::getOutTradeNo, like));
        }
        wrapper.orderByDesc(PayCallbackRecordDO::getId);
        long actualPage = Math.max(1, page);
        long actualSize = Math.max(1, size);
        long total = payCallbackRecordMapper.selectCount(wrapper);
        long pages = (total + actualSize - 1) / actualSize;
        List<PayCallbackRecord> records = payCallbackRecordMapper.selectList(wrapper.last("limit " + ((actualPage - 1) * actualSize) + "," + actualSize))
                .stream()
                .map(this::toDomain)
                .toList();
        return new PageResult<>(actualPage, actualSize, total, pages, records);
    }

    @Override
    public void update(PayCallbackRecord payCallbackRecord) {
        payCallbackRecordMapper.updateById(toDO(payCallbackRecord));
    }

    private PayCallbackRecordDO toDO(PayCallbackRecord payCallbackRecord) {
        PayCallbackRecordDO payCallbackRecordDO = new PayCallbackRecordDO();
        payCallbackRecordDO.setId(payCallbackRecord.id());
        payCallbackRecordDO.setChannel(payCallbackRecord.channel());
        payCallbackRecordDO.setCallbackType(payCallbackRecord.callbackType());
        payCallbackRecordDO.setPayOrderNo(payCallbackRecord.payOrderNo());
        payCallbackRecordDO.setRefundNo(payCallbackRecord.refundNo());
        payCallbackRecordDO.setOrderNo(payCallbackRecord.orderNo());
        payCallbackRecordDO.setOutTradeNo(payCallbackRecord.outTradeNo());
        payCallbackRecordDO.setTransactionNo(payCallbackRecord.transactionNo());
        payCallbackRecordDO.setAmountCent(payCallbackRecord.amountCent());
        payCallbackRecordDO.setTradeStatus(payCallbackRecord.tradeStatus());
        payCallbackRecordDO.setSignature(payCallbackRecord.signature());
        payCallbackRecordDO.setVerified(payCallbackRecord.verified());
        payCallbackRecordDO.setProcessStatus(payCallbackRecord.processStatus());
        payCallbackRecordDO.setFailReason(payCallbackRecord.failReason());
        payCallbackRecordDO.setRawPayload(payCallbackRecord.rawPayload());
        payCallbackRecordDO.setCallbackTime(payCallbackRecord.callbackTime());
        payCallbackRecordDO.setProcessedAt(payCallbackRecord.processedAt());
        payCallbackRecordDO.setCreatedAt(payCallbackRecord.createdAt());
        payCallbackRecordDO.setUpdatedAt(payCallbackRecord.updatedAt());
        return payCallbackRecordDO;
    }

    private PayCallbackRecord toDomain(PayCallbackRecordDO payCallbackRecordDO) {
        return new PayCallbackRecord(
                payCallbackRecordDO.getId(),
                payCallbackRecordDO.getChannel(),
                payCallbackRecordDO.getCallbackType(),
                payCallbackRecordDO.getPayOrderNo(),
                payCallbackRecordDO.getRefundNo(),
                payCallbackRecordDO.getOrderNo(),
                payCallbackRecordDO.getOutTradeNo(),
                payCallbackRecordDO.getTransactionNo(),
                payCallbackRecordDO.getAmountCent(),
                payCallbackRecordDO.getTradeStatus(),
                payCallbackRecordDO.getSignature(),
                Boolean.TRUE.equals(payCallbackRecordDO.getVerified()),
                payCallbackRecordDO.getProcessStatus(),
                payCallbackRecordDO.getFailReason(),
                payCallbackRecordDO.getRawPayload(),
                payCallbackRecordDO.getCallbackTime(),
                payCallbackRecordDO.getProcessedAt(),
                payCallbackRecordDO.getCreatedAt(),
                payCallbackRecordDO.getUpdatedAt()
        );
    }
}
