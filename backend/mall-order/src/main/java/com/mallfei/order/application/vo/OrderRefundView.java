package com.mallfei.order.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "OrderRefundView", description = "订单退款申请视图")
public record OrderRefundView(
        @Schema(description = "退款记录ID", example = "1")
        Long id,
        @Schema(description = "退款状态", example = "REFUND_PENDING")
        String status,
        @Schema(description = "退款原因", example = "商品与描述不符")
        String reason,
        @Schema(description = "申请时间")
        LocalDateTime createdAt
) {
}
