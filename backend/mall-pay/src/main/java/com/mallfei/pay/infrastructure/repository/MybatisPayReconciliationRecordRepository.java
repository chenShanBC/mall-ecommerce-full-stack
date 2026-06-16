package com.mallfei.pay.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallfei.common.api.PageResult;
import com.mallfei.pay.domain.model.PayReconciliationRecord;
import com.mallfei.pay.domain.repository.PayReconciliationRecordRepository;
import com.mallfei.pay.infrastructure.persistence.dataobject.PayReconciliationRecordDO;
import com.mallfei.pay.infrastructure.persistence.mapper.PayReconciliationRecordMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MybatisPayReconciliationRecordRepository implements PayReconciliationRecordRepository {

    private final PayReconciliationRecordMapper payReconciliationRecordMapper;

    public MybatisPayReconciliationRecordRepository(PayReconciliationRecordMapper payReconciliationRecordMapper) {
        this.payReconciliationRecordMapper = payReconciliationRecordMapper;
    }

    @Override
    public PayReconciliationRecord save(PayReconciliationRecord record) {
        PayReconciliationRecordDO recordDO = toDO(record);
        payReconciliationRecordMapper.insert(recordDO);
        return toDomain(recordDO);
    }

    @Override
    public void update(PayReconciliationRecord record) {
        payReconciliationRecordMapper.updateById(toDO(record));
    }

    @Override
    public Optional<PayReconciliationRecord> findById(Long id) {
        return Optional.ofNullable(payReconciliationRecordMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public PageResult<PayReconciliationRecord> search(String bizType, Boolean consistent, String repairStatus, String keyword, long page, long size) {
        LambdaQueryWrapper<PayReconciliationRecordDO> wrapper = baseWrapper(bizType, consistent, repairStatus);
        if (keyword != null && !keyword.isBlank()) {
            String safeKeyword = keyword.trim();
            wrapper.and(w -> w.like(PayReconciliationRecordDO::getOrderNo, safeKeyword)
                    .or().like(PayReconciliationRecordDO::getBatchNo, safeKeyword)
                    .or().like(PayReconciliationRecordDO::getPayOrderNo, safeKeyword)
                    .or().like(PayReconciliationRecordDO::getRefundNo, safeKeyword));
        }
        wrapper.orderByDesc(PayReconciliationRecordDO::getId);
        Page<PayReconciliationRecordDO> result = payReconciliationRecordMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)), wrapper);
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(), result.getRecords().stream().map(this::toDomain).toList());
    }

    @Override
    public long count(String bizType, Boolean consistent, String repairStatus) {
        return payReconciliationRecordMapper.selectCount(baseWrapper(bizType, consistent, repairStatus));
    }

    @Override
    public java.util.List<PayReconciliationRecord> findPendingByOrderNoAndDiffType(String orderNo, String diffType, int limit) {
        LambdaQueryWrapper<PayReconciliationRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PayReconciliationRecordDO::getOrderNo, orderNo)
                .eq(PayReconciliationRecordDO::getDiffType, diffType)
                .eq(PayReconciliationRecordDO::getRepairStatus, PayReconciliationRecord.REPAIR_PENDING)
                .orderByDesc(PayReconciliationRecordDO::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100)));
        return payReconciliationRecordMapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    private LambdaQueryWrapper<PayReconciliationRecordDO> baseWrapper(String bizType, Boolean consistent, String repairStatus) {
        LambdaQueryWrapper<PayReconciliationRecordDO> wrapper = new LambdaQueryWrapper<>();
        if (bizType != null && !bizType.isBlank()) {
            wrapper.eq(PayReconciliationRecordDO::getBizType, bizType.trim().toUpperCase());
        }
        if (consistent != null) {
            wrapper.eq(PayReconciliationRecordDO::getConsistent, consistent);
        }
        if (repairStatus != null && !repairStatus.isBlank()) {
            wrapper.eq(PayReconciliationRecordDO::getRepairStatus, repairStatus.trim().toUpperCase());
        }
        return wrapper;
    }

    private PayReconciliationRecordDO toDO(PayReconciliationRecord record) {
        PayReconciliationRecordDO recordDO = new PayReconciliationRecordDO();
        recordDO.setId(record.id());
        recordDO.setBatchNo(record.batchNo());
        recordDO.setBizType(record.bizType());
        recordDO.setOrderNo(record.orderNo());
        recordDO.setPayOrderNo(record.payOrderNo());
        recordDO.setRefundNo(record.refundNo());
        recordDO.setLocalStatus(record.localStatus());
        recordDO.setChannelStatus(record.channelStatus());
        recordDO.setLocalAmountCent(record.localAmountCent());
        recordDO.setChannelAmountCent(record.channelAmountCent());
        recordDO.setConsistent(record.consistent());
        recordDO.setDiffType(record.diffType());
        recordDO.setRepairStatus(record.repairStatus());
        recordDO.setRemark(record.remark());
        recordDO.setRepairedAt(record.repairedAt());
        recordDO.setCreatedAt(record.createdAt());
        recordDO.setUpdatedAt(record.updatedAt());
        return recordDO;
    }

    private PayReconciliationRecord toDomain(PayReconciliationRecordDO recordDO) {
        return new PayReconciliationRecord(
                recordDO.getId(),
                recordDO.getBatchNo(),
                recordDO.getBizType(),
                recordDO.getOrderNo(),
                recordDO.getPayOrderNo(),
                recordDO.getRefundNo(),
                recordDO.getLocalStatus(),
                recordDO.getChannelStatus(),
                recordDO.getLocalAmountCent(),
                recordDO.getChannelAmountCent(),
                recordDO.getConsistent(),
                recordDO.getDiffType(),
                recordDO.getRepairStatus(),
                recordDO.getRemark(),
                recordDO.getRepairedAt(),
                recordDO.getCreatedAt(),
                recordDO.getUpdatedAt()
        );
    }
}
