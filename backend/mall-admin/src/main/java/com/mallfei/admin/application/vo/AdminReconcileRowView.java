package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminReconcileRowView", description = "后台对账行视图")
public record AdminReconcileRowView(
        @Schema(description = "订单ID", example = "1")
        Long orderId,
        @Schema(description = "订单号", example = "ORD202605010001")
        String orderNo,
        @Schema(description = "订单状态", example = "PAID")
        String orderStatus,
        @Schema(description = "订单支付金额，单位分", example = "9990")
        Long orderPayAmount,
        @Schema(description = "是否存在支付单", example = "true")
        boolean payExists,
        @Schema(description = "支付单号", example = "PAY202605010001")
        String payOrderNo,
        @Schema(description = "支付单状态", example = "SUCCESS")
        String payStatus,
        @Schema(description = "支付金额，单位分", example = "9990")
        Long payAmount,
        @Schema(description = "金额是否一致", example = "true")
        boolean amountConsistent,
        @Schema(description = "状态是否一致", example = "true")
        boolean statusConsistent,
        @Schema(description = "对账结果", example = "CONSISTENT")
        String reconcileStatus
) {
}
