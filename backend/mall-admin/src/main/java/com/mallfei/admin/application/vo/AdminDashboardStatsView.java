package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminDashboardStatsView", description = "后台看板统计视图")
public record AdminDashboardStatsView(
        @Schema(description = "订单总数", example = "10")
        long totalOrderCount,
        @Schema(description = "今日订单数", example = "3")
        long todayOrderCount,
        @Schema(description = "待支付订单数", example = "2")
        long pendingOrderCount,
        @Schema(description = "已支付订单数", example = "3")
        long paidOrderCount,
        @Schema(description = "已发货订单数", example = "2")
        long shippedOrderCount,
        @Schema(description = "已完成订单数", example = "2")
        long completedOrderCount,
        @Schema(description = "已取消订单数", example = "1")
        long cancelledOrderCount,
        @Schema(description = "支付异常订单数", example = "1")
        long paymentExceptionOrderCount,
        @Schema(description = "总销售额，单位分", example = "99900")
        long totalSalesAmount,
        @Schema(description = "支付单总数", example = "8")
        long payOrderCount,
        @Schema(description = "待支付单数", example = "2")
        long pendingPayCount,
        @Schema(description = "成功支付单数", example = "5")
        long successPayCount,
        @Schema(description = "已关闭支付单数", example = "1")
        long closedPayCount
) {
}
