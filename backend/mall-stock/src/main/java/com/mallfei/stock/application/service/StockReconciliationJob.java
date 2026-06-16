package com.mallfei.stock.application.service;

import com.mallfei.stock.domain.model.Stock;
import com.mallfei.stock.domain.model.StockReconciliationRecord;
import com.mallfei.stock.domain.repository.StockReconciliationRecordRepository;
import com.mallfei.stock.domain.service.StockDomainService;
import com.mallfei.stock.infrastructure.config.StockReconciliationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(StockReconciliationProperties.class)
public class StockReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(StockReconciliationJob.class);

    private final StockDomainService stockDomainService;
    private final StockApplicationService stockApplicationService;
    private final StockReconciliationRecordRepository stockReconciliationRecordRepository;
    private final StockReconciliationProperties properties;

    public StockReconciliationJob(StockDomainService stockDomainService,
                                  StockApplicationService stockApplicationService,
                                  StockReconciliationRecordRepository stockReconciliationRecordRepository,
                                  StockReconciliationProperties properties) {
        this.stockDomainService = stockDomainService;
        this.stockApplicationService = stockApplicationService;
        this.stockReconciliationRecordRepository = stockReconciliationRecordRepository;
        this.properties = properties;
    }

    @Scheduled(cron = "${mall.stock.reconciliation.day-cron:0 0 */2 * * ?}")
    public void autoCheckStockConsistencyInBusinessHours() {
        runAutoCheck("DAY", Math.max(1, properties.getDayMaxSkuPerRun()));
    }

    @Scheduled(cron = "${mall.stock.reconciliation.night-cron:0 */15 0-6 * * ?}")
    public void autoCheckStockConsistencyInOffPeakHours() {
        runAutoCheck("NIGHT", Math.max(1, properties.getNightMaxSkuPerRun()));
    }

    private void runAutoCheck(String scheduleType, int maxSkuPerRun) {
        if (!properties.isEnabled()) return;
        int checked = 0;
        int recorded = 0;
        int skipped = 0;
        for (Stock stock : stockDomainService.loadAllStocks()) {
            if (checked >= maxSkuPerRun) break;
            try {
                StockReconciliationRecord record = stockApplicationService.createReconciliationRecordForJob(stock.skuId(), properties.isOnlyRecordInconsistent(), properties.isSkipPendingInconsistent());
                checked++;
                if (record == null) {
                    skipped++;
                } else {
                    recorded++;
                }
            } catch (Exception e) {
                log.warn("库存自动对账校验单SKU失败: scheduleType={}, skuId={}, message={}", scheduleType, stock.skuId(), e.getMessage(), e);
            }
        }
        log.info("库存自动对账校验完成: scheduleType={}, checked={}, recorded={}, skipped={}, maxSkuPerRun={}", scheduleType, checked, recorded, skipped, maxSkuPerRun);
    }
}
