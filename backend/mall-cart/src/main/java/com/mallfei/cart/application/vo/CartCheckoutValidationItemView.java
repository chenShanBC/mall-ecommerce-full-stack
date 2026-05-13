package com.mallfei.cart.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CartCheckoutValidationItemView", description = "结算前校验商品项")
public record CartCheckoutValidationItemView(
        @Schema(description = "购物车项ID", example = "1")
        Long cartItemId,
        @Schema(description = "SKU ID", example = "1")
        Long skuId,
        @Schema(description = "商品名称", example = "手测商品A")
        String productName,
        @Schema(description = "SKU名称", example = "黑色 256G")
        String skuName,
        @Schema(description = "数量", example = "2")
        Integer quantity,
        @Schema(description = "当前单价，单位分", example = "9990")
        Long unitPrice,
        @Schema(description = "是否通过校验", example = "true")
        boolean passed,
        @Schema(description = "失败原因", example = "库存不足")
        String message
) {
}
