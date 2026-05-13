package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminPaySummaryView", description = "后台支付单摘要视图")
public record AdminPaySummaryView(
        @Schema(description = "支付单ID", example = "1")
        Long id,
        @Schema(description = "支付单号", example = "PAY202605010001")
        String payOrderNo,
        @Schema(description = "订单号", example = "ORD202605010001")
        String orderNo,
        @Schema(description = "用户ID", example = "1")
        Long userId,
        @Schema(description = "支付状态", example = "SUCCESS")
        String status,
        @Schema(description = "支付金额，单位分", example = "9990")
        Long payAmount,
        @Schema(description = "支付渠道", example = "MOCK_PAY")
        String payChannel
) {
}
