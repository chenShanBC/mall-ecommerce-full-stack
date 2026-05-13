package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "AdminDashboardOverviewView", description = "后台看板总览视图")
public record AdminDashboardOverviewView(
        @Schema(description = "订单与支付统计")
        AdminDashboardStatsView stats,
        @Schema(description = "库存预警统计")
        AdminStockWarningStatsView stockWarningStats,
        @Schema(description = "对账异常数量", example = "2")
        long abnormalReconcileCount,
        @Schema(description = "待关注事项")
        List<AdminDashboardTodoView> todos,
        @Schema(description = "快捷入口")
        List<AdminDashboardShortcutView> shortcuts
) {
}
