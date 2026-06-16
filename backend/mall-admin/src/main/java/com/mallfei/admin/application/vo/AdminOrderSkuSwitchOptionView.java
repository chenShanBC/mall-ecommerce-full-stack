package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminOrderSkuSwitchOptionView", description = "后台订单换SKU候选规格视图")
public record AdminOrderSkuSwitchOptionView(
        @Schema(description = "SKU ID", example = "12")
        Long skuId,
        @Schema(description = "SKU名称", example = "红色 / XL")
        String skuName,
        @Schema(description = "SKU编码", example = "SKU-RED-XL")
        String skuCode,
        @Schema(description = "规格JSON", example = "{\"color\":\"red\",\"size\":\"XL\"}")
        String specJson,
        @Schema(description = "销售价，单位分", example = "9990")
        Long salePriceCent,
        @Schema(description = "可用库存", example = "100")
        Integer availableStock,
        @Schema(description = "SKU状态", example = "ONLINE")
        String status,
        @Schema(description = "是否同价")
        Boolean samePrice
) {
}
