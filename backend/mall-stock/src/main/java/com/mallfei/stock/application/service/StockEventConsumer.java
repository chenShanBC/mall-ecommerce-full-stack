package com.mallfei.stock.application.service;

import com.mallfei.stock.application.dto.StockSyncEvent;
import com.mallfei.stock.config.StockMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class StockEventConsumer {

    private final StockPersistenceService stockPersistenceService;

    public StockEventConsumer(StockPersistenceService stockPersistenceService) {
        this.stockPersistenceService = stockPersistenceService;
    }

    @RabbitListener(queues = StockMqConfig.STOCK_SYNC_QUEUE)
    public void handleStockSync(StockSyncEvent event) {
        stockPersistenceService.syncReservation(event.businessType(), event.businessNo(), event.skuId(), event.targetStatus());
    }
}
