package com.mallfei.order.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "OrderDetailView", description = "订单详情视图")
public record OrderDetailView(
        @Schema(description = "订单ID", example = "1")
        Long id,
        @Schema(description = "订单号", example = "ORD202605010001")
        String orderNo,
        @Schema(description = "用户ID", example = "1")
        Long userId,
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
        @Schema(description = "订单备注", example = "manual order test")
        String remark,
        @Schema(description = "收货人姓名", example = "Order User")
        String receiverName,
        @Schema(description = "收货人电话", example = "13800138000")
        String receiverPhone,
        @Schema(description = "收货省份", example = "北京市")
        String receiverProvinceName,
        @Schema(description = "收货城市", example = "北京市")
        String receiverCityName,
        @Schema(description = "收货区县", example = "朝阳区")
        String receiverDistrictName,
        @Schema(description = "详细收货地址", example = "望京 SOHO T3 3003")
        String receiverDetailAddress,
        @Schema(description = "支付时间")
        LocalDateTime paidAt,
        @Schema(description = "取消时间")
        LocalDateTime cancelledAt,
        @Schema(description = "发货时间")
        LocalDateTime shippedAt,
        @Schema(description = "完成时间")
        LocalDateTime completedAt,
        @Schema(description = "是否已超时", example = "false")
        boolean expired,
        @Schema(description = "订单超时分钟数", example = "2")
        long timeoutMinutes,
        @Schema(description = "剩余支付秒数", example = "95")
        long remainingPaySeconds,
        @Schema(description = "订单项列表")
        List<OrderItemView> items
) {
}
