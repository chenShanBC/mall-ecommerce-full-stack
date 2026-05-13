package com.mallfei.cart.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "CartItemView", description = "购物车商品项视图")
public record CartItemView(
        @Schema(description = "购物车项ID", example = "1")
        Long id,
        @Schema(description = "SKU ID", example = "1")
        Long skuId,
        @Schema(description = "SPU ID", example = "1")
        Long spuId,
        @Schema(description = "商品名称", example = "手测商品A")
        String productName,
        @Schema(description = "商品主图", example = "https://example.com/a.png")
        String productImage,
        @Schema(description = "SKU名称", example = "黑色 256G")
        String skuName,
        @Schema(description = "规格JSON", example = "{\"color\":\"black\"}")
        String specJson,
        @Schema(description = "单价，单位分", example = "9990")
        Long unitPrice,
        @Schema(description = "数量", example = "2")
        Integer quantity,
        @Schema(description = "小计金额，单位分", example = "19980")
        Long subtotalAmount,
        @Schema(description = "是否勾选", example = "true")
        boolean checked,
        @Schema(description = "是否可结算", example = "true")
        boolean canCheckout,
        @Schema(description = "不可结算原因", example = "商品已下架")
        String invalidReason,
        @Schema(description = "加入时间")
        LocalDateTime createdAt
) {
}
