package com.mallfei.pay.domain.repository;

import com.mallfei.common.api.PageResult;
import com.mallfei.pay.domain.model.PayReconciliationRecord;

import java.util.List;
import java.util.Optional;

public interface PayReconciliationRecordRepository {

    PayReconciliationRecord save(PayReconciliationRecord record);

    void update(PayReconciliationRecord record);

    Optional<PayReconciliationRecord> findById(Long id);

    PageResult<PayReconciliationRecord> search(String bizType, Boolean consistent, String repairStatus, String keyword, long page, long size);

    long count(String bizType, Boolean consistent, String repairStatus);

    List<PayReconciliationRecord> findPendingByOrderNoAndDiffType(String orderNo, String diffType, int limit);
}
