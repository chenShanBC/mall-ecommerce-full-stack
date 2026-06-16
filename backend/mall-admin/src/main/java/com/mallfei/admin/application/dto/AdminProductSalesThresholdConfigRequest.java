package com.mallfei.admin.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminProductSalesThresholdConfigRequest(
        @NotNull(message = "热销阈值不能为空")
        @Min(value = 0, message = "热销阈值不能小于0")
        Integer hotSalesThreshold,
        @NotNull(message = "低销阈值不能为空")
        @Min(value = 0, message = "低销阈值不能小于0")
        Integer lowSalesThreshold
) {
}
