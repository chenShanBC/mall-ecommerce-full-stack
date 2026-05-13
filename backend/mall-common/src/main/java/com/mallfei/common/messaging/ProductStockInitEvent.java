package com.mallfei.common.messaging;

import java.io.Serializable;
import java.util.List;

public record ProductStockInitEvent(
        List<SkuStockInitItem> items
) implements Serializable {

    public record SkuStockInitItem(
            Long skuId,
            Integer initialStock
    ) implements Serializable {
    }
}
