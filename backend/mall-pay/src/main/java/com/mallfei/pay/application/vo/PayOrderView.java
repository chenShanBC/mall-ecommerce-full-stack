package com.mallfei.pay.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PayOrderView", description = "支付单视图")
public record PayOrderView(
        @Schema(description = "支付单号", example = "PAY202605010001")
        String payOrderNo,
        @Schema(description = "订单号", example = "ORD202605010001")
        String orderNo,
        @Schema(description = "支付状态", example = "PENDING")
        String status,
        @Schema(description = "支付金额，单位分", example = "9990")
        Long payAmount,
        @Schema(description = "支付渠道", example = "MOCK_PAY")
        String payChannel,
        @Schema(description = "第三方交易号", example = "MOCK-ORD202605010001")
        String transactionNo,
        @Schema(description = "幂等键", example = "PAY:ORD202605010001")
        String idempotentKey,
        @Schema(description = "创建时间", example = "2026-06-17T10:15:30")
        String createdAt,
        @Schema(description = "回调负载")
        String callbackPayload,
        @Schema(description = "支付跳转表单 HTML")
        String redirectForm,
        @Schema(description = "支付跳转地址")
        String redirectUrl
) {
}
