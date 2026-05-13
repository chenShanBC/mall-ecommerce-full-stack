package com.mallfei.stock.application.service;

import com.mallfei.common.exception.BusinessException;
import com.mallfei.stock.domain.model.Stock;
import com.mallfei.stock.domain.model.StockLockRecord;
import com.mallfei.stock.domain.repository.StockLockRepository;
import com.mallfei.stock.domain.repository.StockRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class StockPersistenceService {

    private final StockRepository stockRepository;
    private final StockLockRepository stockLockRepository;

    public StockPersistenceService(StockRepository stockRepository, StockLockRepository stockLockRepository) {
        this.stockRepository = stockRepository;
        this.stockLockRepository = stockLockRepository;
    }

    public void syncReservation(String businessType, String businessNo, Long skuId, String targetStatus) {
        Stock stock = stockRepository.findBySkuId(skuId)
                .orElseThrow(() -> BusinessException.badRequest("库存不存在: " + skuId));
        StockLockRecord record = stockLockRepository.findByBusiness(businessType, businessNo, skuId)
                .orElseThrow(() -> BusinessException.badRequest("库存锁记录不存在: " + skuId));
        StockLockRecord updatedRecord = record.syncTo(targetStatus, LocalDateTime.now());
        if (Objects.equals(updatedRecord.status(), record.status())) {
            return;
        }

        stockLockRepository.update(updatedRecord);

        Stock updatedStock = stock;
        if (StockLockRecord.STATUS_RESERVED.equals(targetStatus)) {
            updatedStock = stock.reserve(record.quantity());
        } else if (StockLockRecord.STATUS_CANCELLED.equals(targetStatus)) {
            updatedStock = stock.release(record.quantity());
        } else if (StockLockRecord.STATUS_CONFIRMED.equals(targetStatus)) {
            updatedStock = stock.confirm(record.quantity());
        }
        stockRepository.update(updatedStock);
    }
}
