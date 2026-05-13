package com.mallfei.stock.facade;

public record StockOperationResultSnapshot(
        String status,
        String businessType,
        String businessNo,
        String mode
) {
}
