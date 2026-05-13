package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminOrderItemView", description = "后台订单项视图")
public record AdminOrderItemView(
        @Schema(description = "订单项ID", example = "1")
        Long id,
        @Schema(description = "SKU ID", example = "1")
        Long skuId,
        @Schema(description = "SKU名称", example = "默认规格")
        String skuName,
        @Schema(description = "数量", example = "1")
        Integer quantity,
        @Schema(description = "销售价，单位分", example = "9990")
        Long salePrice,
        @Schema(description = "小计金额，单位分", example = "9990")
        Long totalAmount
) {
}
