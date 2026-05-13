package com.mallfei.stock.config;

import com.mallfei.common.messaging.ProductStockInitMqContract;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StockMqConfig {

    public static final String STOCK_EVENT_EXCHANGE = "stock.event.exchange";
    public static final String STOCK_SYNC_QUEUE = "stock.sync.queue";
    public static final String STOCK_SYNC_ROUTING_KEY = "stock.sync";

    @Bean
    public TopicExchange stockEventExchange() {
        return new TopicExchange(STOCK_EVENT_EXCHANGE);
    }

    @Bean
    public Queue stockSyncQueue() {
        return new Queue(STOCK_SYNC_QUEUE, true);
    }

    @Bean
    public Binding stockSyncBinding() {
        return BindingBuilder.bind(stockSyncQueue()).to(stockEventExchange()).with(STOCK_SYNC_ROUTING_KEY);
    }

    @Bean
    public TopicExchange productEventExchange() {
        return new TopicExchange(ProductStockInitMqContract.PRODUCT_EVENT_EXCHANGE);
    }

    @Bean
    public Queue productStockInitQueue() {
        return new Queue(ProductStockInitMqContract.PRODUCT_STOCK_INIT_QUEUE, true);
    }

    @Bean
    public Binding productStockInitBinding() {
        return BindingBuilder.bind(productStockInitQueue()).to(productEventExchange()).with(ProductStockInitMqContract.PRODUCT_STOCK_INIT_ROUTING_KEY);
    }
}
