package com.mallfei.cart.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CartQuantityView", description = "购物车数量统计")
public record CartQuantityView(
        @Schema(description = "购物车商品项数", example = "3")
        Integer itemCount,
        @Schema(description = "购物车商品总件数", example = "6")
        Integer totalQuantity
) {
}
