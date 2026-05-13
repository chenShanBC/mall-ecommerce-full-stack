package com.mallfei.stock.application.dto;

import java.io.Serializable;

public record StockSyncEvent(
        String businessType,
        String businessNo,
        Long skuId,
        String targetStatus
) implements Serializable {
}
