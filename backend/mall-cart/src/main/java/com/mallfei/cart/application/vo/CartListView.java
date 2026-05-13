package com.mallfei.cart.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "CartListView", description = "购物车列表视图")
public record CartListView(
        @Schema(description = "购物车商品项列表")
        List<CartItemView> items,
        @Schema(description = "购物车商品总数", example = "3")
        Integer itemCount,
        @Schema(description = "购物车商品总件数", example = "6")
        Integer totalQuantity,
        @Schema(description = "已勾选商品合计金额，单位分", example = "29970")
        Long checkedTotalAmount
) {
}
