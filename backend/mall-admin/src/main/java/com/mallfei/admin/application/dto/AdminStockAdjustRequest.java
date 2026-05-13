package com.mallfei.admin.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminStockAdjustRequest(
        @NotNull(message = "调整后总库存不能为空") @Min(value = 0, message = "总库存不能小于0") Integer totalStock,
        @NotNull(message = "调整后可用库存不能为空") @Min(value = 0, message = "可用库存不能小于0") Integer availableStock,
        @NotNull(message = "调整后锁定库存不能为空") @Min(value = 0, message = "锁定库存不能小于0") Integer lockedStock,
        @NotBlank(message = "调整原因不能为空") String reason
) {
}
