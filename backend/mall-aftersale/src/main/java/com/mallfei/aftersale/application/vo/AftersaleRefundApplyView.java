package com.mallfei.aftersale.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "AftersaleRefundApplyView", description = "售后仅退款申请视图")
public record AftersaleRefundApplyView(
        @Schema(description = "售后单ID", example = "1")
        Long id,
        @Schema(description = "售后单号", example = "AFT202605090001")
        String aftersaleNo,
        @Schema(description = "订单号", example = "ORD202605090001")
        String orderNo,
        @Schema(description = "售后状态", example = "PENDING_REVIEW")
        String status,
        @Schema(description = "退款原因", example = "商品与描述不符")
        String reason,
        @Schema(description = "申请时间")
        LocalDateTime createdAt
) {
}
