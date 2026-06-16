package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "AdminAftersaleSummaryView", description = "后台售后单摘要视图")
public record AdminAftersaleSummaryView(
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
        @Schema(description = "用户申请原因", example = "商品与描述不符")
        String reason,
        @Schema(description = "商家驳回原因", example = "商品已影响二次销售")
        String rejectReason,
        @Schema(description = "退款失败原因", example = "渠道退款失败")
        String failReason,
        @Schema(description = "创建时间")
        LocalDateTime createdAt
) {
}
