package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminPayDetailView", description = "后台支付单详情视图")
public record AdminPayDetailView(
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
        String payChannel,
        @Schema(description = "第三方交易号", example = "MOCK-ORD202605010001")
        String transactionNo,
        @Schema(description = "幂等键", example = "PAY:ORD202605010001")
        String idempotentKey,
        @Schema(description = "回调负载")
        String callbackPayload
) {
}
