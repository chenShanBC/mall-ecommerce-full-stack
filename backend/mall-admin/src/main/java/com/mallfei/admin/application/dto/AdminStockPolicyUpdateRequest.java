package com.mallfei.admin.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminStockPolicyUpdateRequest(
        @NotBlank(message = "库存状态不能为空") String stockStatus,
        @NotNull(message = "低阈值不能为空") @Min(value = 0, message = "低阈值不能小于0") Integer lowStockThreshold,
        @NotNull(message = "高阈值不能为空") @Min(value = 0, message = "高阈值不能小于0") Integer highStockThreshold,
        @NotBlank(message = "调整原因不能为空") String reason
) {
}
