package com.mallfei.cart.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "CartCheckoutValidationView", description = "结算前校验结果")
public record CartCheckoutValidationView(
        @Schema(description = "是否全部通过", example = "true")
        boolean passed,
        @Schema(description = "提示消息", example = "校验通过")
        String message,
        @Schema(description = "校验商品项列表")
        List<CartCheckoutValidationItemView> items,
        @Schema(description = "合计金额，单位分", example = "19980")
        Long totalAmount
) {
}
