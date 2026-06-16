package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "AdminDashboardOverviewView", description = "后台看板总览视图")
public record AdminDashboardOverviewView(
        @Schema(description = "订单与支付统计")
        AdminDashboardStatsView stats,
        @Schema(description = "库存预警统计")
        AdminStockWarningStatsView stockWarningStats,
        @Schema(description = "最新库存变更日志")
        List<AdminStockOperationLogView> recentStockOperationLogs,
        @Schema(description = "运营售后待处理列表")
        List<AdminAftersaleSummaryView> pendingAftersales,
        @Schema(description = "商品运营统计")
        AdminProductOperationStatsView productStats,
        @Schema(description = "近几个月商品销量趋势")
        List<AdminProductSalesMonthlyTrendView> productSalesMonthlyTrend,
        @Schema(description = "运营近 7 日订单/履约/售后趋势")
        List<AdminDashboardOperationsTrendView> operationsTrend,
        @Schema(description = "对账异常总数量", example = "2")
        long abnormalReconcileCount,
        @Schema(description = "支付对账异常数量", example = "1")
        long payAbnormalReconcileCount,
        @Schema(description = "库存对账异常数量", example = "1")
        long stockAbnormalReconcileCount,
        @Schema(description = "待关注事项")
        List<AdminDashboardTodoView> todos,
        @Schema(description = "快捷入口")
        List<AdminDashboardShortcutView> shortcuts
) {
}
