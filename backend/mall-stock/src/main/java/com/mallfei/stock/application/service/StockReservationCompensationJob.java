package com.mallfei.stock.application.service;

import com.mallfei.stock.domain.model.StockLockRecord;
import com.mallfei.stock.domain.repository.StockLockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StockReservationCompensationJob {

    private static final Logger log = LoggerFactory.getLogger(StockReservationCompensationJob.class);
    private static final int DEFAULT_SYNC_TIMEOUT_MINUTES = 3;
    private static final int DEFAULT_SCAN_LIMIT = 100;

    private final StockLockRepository stockLockRepository;
    private final StockEventPublisher stockEventPublisher;
    private final StringRedisTemplate redisTemplate;

    public StockReservationCompensationJob(StockLockRepository stockLockRepository,
                                           StockEventPublisher stockEventPublisher,
                                           StringRedisTemplate redisTemplate) {
        this.stockLockRepository = stockLockRepository;
        this.stockEventPublisher = stockEventPublisher;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedDelay = 120_000L, initialDelay = 180_000L)
    public void republishUnpersistedReservations() {
        for (StockLockRecord record : stockLockRepository.findUnpersistedReservations(DEFAULT_SYNC_TIMEOUT_MINUTES, DEFAULT_SCAN_LIMIT)) {
            if (!redisReservationExists(record)) {
                log.warn("库存锁记录未完成DB同步，但Redis预占状态不存在，等待人工对账: businessType={}, businessNo={}, skuId={}", record.businessType(), record.businessNo(), record.skuId());
                continue;
            }
            stockEventPublisher.publishSyncEvent(new com.mallfei.stock.application.dto.StockSyncEvent(record.businessType(), record.businessNo(), record.skuId(), StockLockRecord.STATUS_RESERVED));
        }
    }

    private boolean redisReservationExists(StockLockRecord record) {
        String key = "stock:reservation:" + record.businessType() + ":" + record.businessNo() + ":" + record.skuId();
        Object status = redisTemplate.opsForHash().get(key, "status");
        return StockLockRecord.STATUS_RESERVED.equals(status);
    }
}
