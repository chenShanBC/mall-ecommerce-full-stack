package com.mallfei.stock.application.dto;

public record StockHealthView(
        String module,
        String status,
        String mode
) {
}
