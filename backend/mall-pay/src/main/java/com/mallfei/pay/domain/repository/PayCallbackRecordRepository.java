package com.mallfei.pay.domain.repository;

import com.mallfei.common.api.PageResult;
import com.mallfei.pay.domain.model.PayCallbackRecord;

import java.util.Optional;

public interface PayCallbackRecordRepository {

    PayCallbackRecord save(PayCallbackRecord payCallbackRecord);

    Optional<PayCallbackRecord> findLatestByTransactionNo(String transactionNo);

    Optional<PayCallbackRecord> findLatestByOutTradeNo(String outTradeNo);

    PageResult<PayCallbackRecord> search(String processStatus, String keyword, long page, long size);

    void update(PayCallbackRecord payCallbackRecord);
}
