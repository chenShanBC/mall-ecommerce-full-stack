package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminStockWarningStatsView", description = "后台库存预警统计视图")
public record AdminStockWarningStatsView(
        @Schema(description = "LOW预警数量", example = "3")
        long lowCount,
        @Schema(description = "HIGH预警数量", example = "1")
        long highCount,
        @Schema(description = "NORMAL数量", example = "20")
        long normalCount,
        @Schema(description = "总SKU数量", example = "24")
        long totalCount
) {
}
