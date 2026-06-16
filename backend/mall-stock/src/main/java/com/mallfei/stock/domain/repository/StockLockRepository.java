package com.mallfei.stock.domain.repository;

import com.mallfei.stock.domain.model.StockLockRecord;

import java.util.List;
import java.util.Optional;

public interface StockLockRepository {

    Optional<StockLockRecord> findByBusiness(String businessType, String businessNo, Long skuId);

    StockLockRecord save(StockLockRecord stockLockRecord);

    void update(StockLockRecord stockLockRecord);

    List<StockLockRecord> findByBusiness(String businessType, String businessNo);

    List<StockLockRecord> findExpiredReserved(int minutes);

    List<StockLockRecord> findUnpersistedReservations(int minutes, int limit);

    long sumReservedQuantity(Long skuId);

    long sumUnpersistedReservedQuantity(Long skuId);
}
