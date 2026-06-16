package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminDashboardOperationsTrendView", description = "后台运营看板按日趋势视图")
public record AdminDashboardOperationsTrendView(
        @Schema(description = "日期，格式 yyyy-MM-dd", example = "2026-06-17")
        String date,
        @Schema(description = "订单总量", example = "31")
        long orderCount,
        @Schema(description = "履约完成订单量", example = "10")
        long completedOrderCount,
        @Schema(description = "售后单量", example = "3")
        long aftersaleCount
) {
}
