package com.mallfei.stock.application.service;

import com.mallfei.stock.application.dto.StockSyncEvent;
import com.mallfei.stock.config.StockMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class StockEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public StockEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishSyncEvent(StockSyncEvent event) {
        rabbitTemplate.convertAndSend(StockMqConfig.STOCK_EVENT_EXCHANGE, StockMqConfig.STOCK_SYNC_ROUTING_KEY, event);
    }
}
