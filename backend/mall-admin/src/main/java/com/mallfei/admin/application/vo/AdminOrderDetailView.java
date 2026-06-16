package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "AdminOrderDetailView", description = "后台订单详情视图")
public record AdminOrderDetailView(
        @Schema(description = "订单ID", example = "1")
        Long id,
        @Schema(description = "订单号", example = "ORD202605010001")
        String orderNo,
        @Schema(description = "用户ID", example = "1")
        Long userId,
        @Schema(description = "订单状态", example = "PAID")
        String status,
        @Schema(description = "订单总金额，单位分", example = "9990")
        Long totalAmount,
        @Schema(description = "支付金额，单位分", example = "9990")
        Long payAmount,
        @Schema(description = "收货人", example = "张三")
        String receiverName,
        @Schema(description = "商品数量", example = "1")
        Integer itemCount,
        @Schema(description = "收货电话", example = "13800138000")
        String receiverPhone,
        @Schema(description = "收货省份", example = "北京市")
        String receiverProvinceName,
        @Schema(description = "收货城市", example = "北京市")
        String receiverCityName,
        @Schema(description = "收货区县", example = "朝阳区")
        String receiverDistrictName,
        @Schema(description = "收货详细地址", example = "望京 SOHO T3 3003")
        String receiverDetailAddress,
        @Schema(description = "完整地址", example = "北京市北京市朝阳区望京 SOHO T3 3003")
        String address,
        @Schema(description = "订单备注", example = "manual order test")
        String remark,
        @Schema(description = "支付时间")
        LocalDateTime paidAt,
        @Schema(description = "取消时间")
        LocalDateTime cancelledAt,
        @Schema(description = "发货时间")
        LocalDateTime shippedAt,
        @Schema(description = "完成时间")
        LocalDateTime completedAt,
        @Schema(description = "订单项列表")
        List<AdminOrderItemView> items
) {
}
