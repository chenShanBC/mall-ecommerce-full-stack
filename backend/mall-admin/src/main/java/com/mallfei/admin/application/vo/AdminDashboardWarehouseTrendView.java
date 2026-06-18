package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminDashboardWarehouseTrendView", description = "后台仓储库存健康与出库趋势视图")
public record AdminDashboardWarehouseTrendView(
        @Schema(description = "日期", example = "2026-06-17")
        String date,
        @Schema(description = "发货件数", example = "35")
        Long shippedItemCount,
        @Schema(description = "发货订单数", example = "12")
        Long shippedOrderCount,
        @Schema(description = "库存策略配置调整次数", example = "3")
        Long stockPolicyUpdateCount
) {
}
