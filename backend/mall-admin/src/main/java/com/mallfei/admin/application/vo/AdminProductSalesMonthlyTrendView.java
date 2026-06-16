package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminProductSalesMonthlyTrendView", description = "商品月度销量趋势视图")
public record AdminProductSalesMonthlyTrendView(
        @Schema(description = "月份，格式 yyyy-MM", example = "2026-06")
        String month,
        @Schema(description = "完成销量", example = "128")
        long salesCount,
        @Schema(description = "完成销售额，单位分", example = "1299000")
        long salesAmountCent
) {
}
