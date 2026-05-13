package com.mallfei.stock.application.service;

import com.mallfei.common.messaging.ProductStockInitEvent;
import com.mallfei.common.messaging.ProductStockInitMqContract;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ProductStockInitEventConsumer {

    private final StockApplicationService stockApplicationService;

    public ProductStockInitEventConsumer(StockApplicationService stockApplicationService) {
        this.stockApplicationService = stockApplicationService;
    }

    @RabbitListener(queues = ProductStockInitMqContract.PRODUCT_STOCK_INIT_QUEUE)
    public void handle(ProductStockInitEvent event) {
        if (event == null || event.items() == null) {
            return;
        }
        event.items().forEach(item -> {
            if (item != null && item.skuId() != null && item.initialStock() != null) {
                stockApplicationService.initStock(item.skuId(), item.initialStock());
            }
        });
    }
}
