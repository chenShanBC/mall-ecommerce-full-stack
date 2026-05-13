package com.mallfei.product.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminProductSummaryView", description = "后台商品摘要视图")
public record AdminProductSummaryView(
        @Schema(description = "商品ID", example = "1")
        Long id,
        @Schema(description = "商品名称", example = "手测商品A")
        String name,
        @Schema(description = "商品状态", example = "ONLINE")
        String status,
        @Schema(description = "SKU数量", example = "1")
        Integer skuCount
) {
}
