package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminDashboardFinanceTrendView", description = "后台财务趋势视图")
public record AdminDashboardFinanceTrendView(
        @Schema(description = "日期", example = "2026-06-17")
        String date,
        @Schema(description = "支付完成金额，单位分", example = "254000")
        Long paidAmountCent,
        @Schema(description = "退款成功金额，单位分", example = "37000")
        Long refundAmountCent,
        @Schema(description = "净收入，单位分", example = "217000")
        Long netIncomeCent,
        @Schema(description = "待处理对账差异数", example = "1")
        Long pendingDiffCount
) {
}
