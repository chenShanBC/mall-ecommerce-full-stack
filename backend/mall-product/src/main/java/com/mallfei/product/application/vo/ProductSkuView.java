package com.mallfei.product.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProductSkuView", description = "商品SKU视图")
public record ProductSkuView(
        @Schema(description = "SKU ID", example = "11")
        Long id,
        @Schema(description = "SKU编码", example = "TEST-SKU-001")
        String skuCode,
        @Schema(description = "SKU名称", example = "默认规格")
        String skuName,
        @Schema(description = "规格JSON", example = "{\"color\":\"black\"}")
        String specJson,
        @Schema(description = "销售价，单位分", example = "9990")
        Long salePrice,
        @Schema(description = "原价，单位分", example = "12990")
        Long originPrice,
        @Schema(description = "状态", example = "ONLINE")
        String status,
        @Schema(description = "销量", example = "100")
        Integer sales,
        @Schema(description = "可售库存", example = "100")
        Integer availableStock
) {
}
