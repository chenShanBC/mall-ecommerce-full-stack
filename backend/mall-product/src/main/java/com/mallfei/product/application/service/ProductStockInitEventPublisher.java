package com.mallfei.product.application.service;

import com.mallfei.common.messaging.ProductStockInitEvent;
import com.mallfei.common.messaging.ProductStockInitMqContract;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductStockInitEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ProductStockInitEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(ProductStockInitEvent event) {
        rabbitTemplate.convertAndSend(ProductStockInitMqContract.PRODUCT_EVENT_EXCHANGE, ProductStockInitMqContract.PRODUCT_STOCK_INIT_ROUTING_KEY, event);
    }
}
