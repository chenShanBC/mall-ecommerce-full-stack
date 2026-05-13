package com.mallfei.cart.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CartSettlementPreviewItemView", description = "结算预览商品项")
public record CartSettlementPreviewItemView(
        @Schema(description = "购物车项ID", example = "1")
        Long cartItemId,
        @Schema(description = "SKU ID", example = "1")
        Long skuId,
        @Schema(description = "SKU名称", example = "默认规格")
        String skuName,
        @Schema(description = "购买数量", example = "1")
        Integer quantity,
        @Schema(description = "单价，单位分", example = "9990")
        Long unitPrice,
        @Schema(description = "小计金额，单位分", example = "9990")
        Long subtotalAmount
) {
}
