package com.mallfei.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(name = "OrderCreateRequest", description = "创建订单请求")
public record OrderCreateRequest(
        @Schema(description = "收货人姓名", example = "Order User")
        @NotBlank(message = "收货人不能为空") String receiverName,
        @Schema(description = "收货人电话", example = "13800138000")
        @NotBlank(message = "收货电话不能为空") String receiverPhone,
        @Schema(description = "收货省份", example = "北京市")
        @NotBlank(message = "省不能为空") String receiverProvinceName,
        @Schema(description = "收货城市", example = "北京市")
        @NotBlank(message = "市不能为空") String receiverCityName,
        @Schema(description = "收货区县", example = "朝阳区")
        @NotBlank(message = "区不能为空") String receiverDistrictName,
        @Schema(description = "详细收货地址", example = "望京 SOHO T3 3003")
        @NotBlank(message = "详细地址不能为空") String receiverDetailAddress,
        @Schema(description = "订单备注", example = "manual order test")
        String remark,
        @Schema(description = "订单项列表")
        @NotNull(message = "订单项不能为空") List<Item> items
) {
    @Schema(name = "OrderCreateItem", description = "创建订单的订单项")
    public record Item(
            @Schema(description = "SKU ID", example = "1")
            @NotNull(message = "SKU不能为空") Long skuId,
            @Schema(description = "购买数量", example = "1")
            @NotNull(message = "数量不能为空") @Min(value = 1, message = "数量必须大于0") Integer quantity
    ) {
    }
}
