package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "AdminAftersaleDetailView", description = "后台售后单详情视图")
public record AdminAftersaleDetailView(
        @Schema(description = "售后单ID", example = "1")
        Long id,
        @Schema(description = "售后单号", example = "AFT202605090001")
        String aftersaleNo,
        @Schema(description = "订单号", example = "ORD202605090001")
        String orderNo,
        @Schema(description = "用户ID", example = "1")
        Long userId,
        @Schema(description = "售后类型", example = "ONLY_REFUND")
        String aftersaleType,
        @Schema(description = "售后状态", example = "PENDING_REVIEW")
        String status,
        @Schema(description = "退款金额，单位分", example = "9900")
        Long refundAmount,
        @Schema(description = "售后原因", example = "商品与描述不符")
        String reason,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}
