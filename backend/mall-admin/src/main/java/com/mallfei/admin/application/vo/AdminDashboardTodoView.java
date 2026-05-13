package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminDashboardTodoView", description = "后台看板待关注事项视图")
public record AdminDashboardTodoView(
        @Schema(description = "标识", example = "order:pending-payment")
        String code,
        @Schema(description = "标题", example = "待支付订单")
        String title,
        @Schema(description = "说明", example = "需要关注支付转化")
        String description,
        @Schema(description = "数量", example = "12")
        long count,
        @Schema(description = "级别", example = "warning")
        String level,
        @Schema(description = "跳转路径", example = "/orders")
        String path,
        @Schema(description = "query 参数 key", example = "status")
        String queryKey,
        @Schema(description = "query 参数 value", example = "PENDING_PAYMENT")
        String queryValue
) {
}
