package com.mallfei.product.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminProductPageRowView", description = "后台商品列表行视图")
public record AdminProductPageRowView(
        @Schema(description = "商品ID", example = "1")
        Long id,
        @Schema(description = "商品名称", example = "手测商品A")
        String name,
        @Schema(description = "类目ID", example = "1")
        Long categoryId,
        @Schema(description = "商品状态", example = "ONLINE")
        String status,
        @Schema(description = "SKU数量", example = "1")
        Integer skuCount,
        @Schema(description = "销售价，单位分", example = "9990")
        Long salePrice,
        @Schema(description = "累计销量", example = "128")
        Integer salesCount,
        @Schema(description = "月销量", example = "38")
        Integer monthlySalesCount,
        @Schema(description = "销售分层：HOT-热销，LOW-低销，NORMAL-常规", example = "HOT")
        String salesBand,
        @Schema(description = "销售分层展示文案", example = "热销")
        String salesBandLabel,
        @Schema(description = "库存", example = "0")
        Integer stock
) {
}
