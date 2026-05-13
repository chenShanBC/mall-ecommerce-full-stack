package com.mallfei.stock.application.dto;

public record StockOperationResult(
        String status,
        String businessType,
        String businessNo,
        String mode
) {
}
