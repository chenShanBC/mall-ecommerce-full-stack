package com.mallfei.order.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "OrderSummaryView", description = "订单摘要视图")
public record OrderSummaryView(
        @Schema(description = "订单ID", example = "1")
        Long id,
        @Schema(description = "订单号", example = "ORD202605010001")
        String orderNo,
        @Schema(description = "订单状态", example = "PENDING_PAYMENT")
        String status,
        @Schema(description = "订单总金额，单位分", example = "9990")
        Long totalAmount,
        @Schema(description = "应付金额，单位分", example = "9990")
        Long payAmount,
        @Schema(description = "运费，单位分", example = "0")
        Long freightAmount,
        @Schema(description = "优惠金额，单位分", example = "0")
        Long discountAmount,
        @Schema(description = "支付方式", example = "MOCK_PAY")
        String payType,
        @Schema(description = "是否已超时", example = "false")
        boolean expired,
        @Schema(description = "订单超时分钟数", example = "2")
        long timeoutMinutes,
        @Schema(description = "剩余支付秒数", example = "95")
        long remainingPaySeconds,
        @Schema(description = "商品数量", example = "1")
        Integer itemCount,
        @Schema(description = "完成时间")
        LocalDateTime completedAt,
        @Schema(description = "首个商品SKU ID", example = "101")
        Long firstSkuId,
        @Schema(description = "首个商品SPU ID", example = "201")
        Long firstSpuId,
        @Schema(description = "首个商品SKU 名称", example = "曜讯 蓝牙音箱 户外版")
        String firstSkuName,
        @Schema(description = "首个商品图片", example = "https://cdn.mallfei.com/product/speaker.png")
        String firstSkuImageUrl,
        @Schema(description = "最新退款单号", example = "ORF202606020001")
        String latestRefundNo,
        @Schema(description = "最新退款状态", example = "REFUND_PENDING")
        String latestRefundStatus
) {
}
