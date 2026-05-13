package com.mallfei.product.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProductCardView", description = "商品卡片视图")
public record ProductCardView(
        @Schema(description = "商品ID", example = "1")
        Long id,
        @Schema(description = "商品名称", example = "手测商品A")
        String name,
        @Schema(description = "商品所属类目ID", example = "10")
        Long categoryId,
        @Schema(description = "商品主图", example = "https://example.com/product-a.png")
        String mainImage,
        @Schema(description = "销售价，单位分", example = "9990")
        Long salePrice,
        @Schema(description = "原价，单位分", example = "12990")
        Long originPrice,
        @Schema(description = "销量", example = "100")
        Integer sales
) {
}
