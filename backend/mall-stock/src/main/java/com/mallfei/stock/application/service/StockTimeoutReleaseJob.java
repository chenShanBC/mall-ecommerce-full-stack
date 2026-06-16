package com.mallfei.stock.application.service;

import com.mallfei.common.exception.BusinessException;
import com.mallfei.stock.domain.model.StockLockRecord;
import com.mallfei.stock.domain.repository.StockLockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StockTimeoutReleaseJob {

    private static final Logger log = LoggerFactory.getLogger(StockTimeoutReleaseJob.class);
    private static final int DEFAULT_LOCK_TIMEOUT_MINUTES = 30;

    private final StockLockRepository stockLockRepository;
    private final StockApplicationService stockApplicationService;
    private final StockPersistenceService stockPersistenceService;

    public StockTimeoutReleaseJob(StockLockRepository stockLockRepository,
                                  StockApplicationService stockApplicationService,
                                  StockPersistenceService stockPersistenceService) {
        this.stockLockRepository = stockLockRepository;
        this.stockApplicationService = stockApplicationService;
        this.stockPersistenceService = stockPersistenceService;
    }

    @Scheduled(fixedDelay = 60_000L, initialDelay = 60_000L)
    public void releaseExpiredReservations() {
        for (StockLockRecord record : stockLockRepository.findExpiredReserved(DEFAULT_LOCK_TIMEOUT_MINUTES)) {
            try {
                stockApplicationService.cancelBusiness(record.businessType(), record.businessNo());
            } catch (BusinessException e) {
                if (e.getMessage() != null && e.getMessage().contains("库存已确认，不能取消")) {
                    stockPersistenceService.syncReservation(record.businessType(), record.businessNo(), record.skuId(), StockLockRecord.STATUS_CONFIRMED);
                    log.warn("过期库存锁已在Redis确认，已同步为确认状态: businessType={}, businessNo={}, skuId={}", record.businessType(), record.businessNo(), record.skuId());
                    continue;
                }
                log.warn("释放过期库存锁失败: businessType={}, businessNo={}, skuId={}, message={}", record.businessType(), record.businessNo(), record.skuId(), e.getMessage());
            }
        }
    }
}
