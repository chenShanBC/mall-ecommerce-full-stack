package com.mallfei.cart.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CartOperationResultView", description = "购物车操作结果")
public record CartOperationResultView(
        @Schema(description = "是否成功", example = "true")
        boolean success,
        @Schema(description = "提示消息", example = "加入购物车成功")
        String message,
        @Schema(description = "购物车数量统计")
        CartQuantityView quantity
) {
}
