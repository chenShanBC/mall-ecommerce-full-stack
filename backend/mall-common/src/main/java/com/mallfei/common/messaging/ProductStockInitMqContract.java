package com.mallfei.common.messaging;

public final class ProductStockInitMqContract {

    public static final String PRODUCT_EVENT_EXCHANGE = "product.event.exchange";
    public static final String PRODUCT_STOCK_INIT_QUEUE = "product.stock.init.queue";
    public static final String PRODUCT_STOCK_INIT_ROUTING_KEY = "product.stock.init";

    private ProductStockInitMqContract() {
    }
}
