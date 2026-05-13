package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminOrderSummaryView", description = "后台订单摘要视图")
public record AdminOrderSummaryView(
        @Schema(description = "订单ID", example = "1") Long id,
        @Schema(description = "订单号", example = "ORD202605010001") String orderNo,
        @Schema(description = "用户ID", example = "1") Long userId,
        @Schema(description = "订单状态", example = "PAID") String status,
        @Schema(description = "订单总金额，单位分", example = "9990") Long totalAmount,
        @Schema(description = "支付金额，单位分", example = "9990") Long payAmount,
        @Schema(description = "收货人", example = "张三") String receiverName,
        @Schema(description = "收货电话", example = "13800138000") String receiverPhone,
        @Schema(description = "商品数量", example = "1") Integer itemCount
) {
}
