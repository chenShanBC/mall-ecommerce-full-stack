package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminReconciliationActionView", description = "后台对账差异可执行处置动作")
public record AdminReconciliationActionView(
        @Schema(description = "动作编码", example = "SYNC_PAY_STATUS")
        String action,
        @Schema(description = "动作名称", example = "同步支付状态")
        String label,
        @Schema(description = "动作说明")
        String description,
        @Schema(description = "按钮类型", example = "primary")
        String type,
        @Schema(description = "是否高风险动作", example = "false")
        boolean danger
) {
}
