package com.mallfei.stock.domain.repository;

import com.mallfei.common.api.PageResult;
import com.mallfei.stock.domain.model.StockReconciliationRecord;

import java.util.Optional;

public interface StockReconciliationRecordRepository {

    StockReconciliationRecord save(StockReconciliationRecord record);

    void update(StockReconciliationRecord record);

    Optional<StockReconciliationRecord> findById(Long id);

    Optional<StockReconciliationRecord> findLatestBySkuId(Long skuId);

    boolean existsPendingInconsistent(Long skuId);

    PageResult<StockReconciliationRecord> page(Long skuId, String status, long page, long size, String sortBy, String sortOrder);
}
