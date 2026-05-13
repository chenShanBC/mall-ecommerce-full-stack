package com.mallfei.stock.application.dto;

import jakarta.validation.constraints.NotNull;

public record StockAdjustRequest(
        @NotNull(message = "调整后总库存不能为空") Integer totalStock,
        @NotNull(message = "调整后可用库存不能为空") Integer availableStock,
        @NotNull(message = "调整后锁定库存不能为空") Integer lockedStock,
        String reason
) {
}
