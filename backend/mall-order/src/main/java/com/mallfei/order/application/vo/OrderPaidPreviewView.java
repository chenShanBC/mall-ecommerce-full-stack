package com.mallfei.order.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OrderPaidPreviewView", description = "订单支付预览视图")
public record OrderPaidPreviewView(
        @Schema(description = "订单号", example = "ORD202605010001")
        String orderNo,
        @Schema(description = "用户ID", example = "1")
        Long userId,
        @Schema(description = "应付金额，单位分", example = "9990")
        Long payAmount,
        @Schema(description = "订单状态", example = "PENDING_PAYMENT")
        String status
) {
}
