package com.mallfei.admin.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AdminStockAdjustRequest(
        String adjustmentType,
        @Min(value = 1, message = "调整数量必须大于0") Integer changeQuantity,
        @Min(value = 0, message = "总库存不能小于0") Integer totalStock,
        @Min(value = 0, message = "可用库存不能小于0") Integer availableStock,
        @Min(value = 0, message = "锁定库存不能小于0") Integer lockedStock,
        @NotBlank(message = "调整原因不能为空") String reason,
        String remark
) {
}
