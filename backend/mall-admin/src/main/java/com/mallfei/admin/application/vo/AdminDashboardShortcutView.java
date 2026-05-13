package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminDashboardShortcutView", description = "后台看板快捷入口视图")
public record AdminDashboardShortcutView(
        @Schema(description = "标题", example = "异常对账")
        String title,
        @Schema(description = "描述", example = "待处理异常对账单")
        String description,
        @Schema(description = "前端路径", example = "/reconciliations")
        String path,
        @Schema(description = "query 参数 key", example = "status")
        String queryKey,
        @Schema(description = "query 参数 value", example = "ABNORMAL")
        String queryValue,
        @Schema(description = "数量", example = "3")
        long count
) {
}
