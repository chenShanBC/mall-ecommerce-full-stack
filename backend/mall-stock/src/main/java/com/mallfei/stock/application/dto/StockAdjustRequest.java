package com.mallfei.stock.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record StockAdjustRequest(
        String adjustmentType,
        @Min(value = 1, message = "调整数量必须大于 0") Integer changeQuantity,
        @Min(value = 0, message = "总库存不能为负数") Integer totalStock,
        @Min(value = 0, message = "可用库存不能为负数") Integer availableStock,
        @Min(value = 0, message = "锁定库存不能为负数") Integer lockedStock,
        @NotBlank(message = "库存调整原因不能为空") String reason,
        String remark
) {
}
