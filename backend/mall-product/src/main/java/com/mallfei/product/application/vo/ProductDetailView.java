package com.mallfei.product.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "ProductDetailView", description = "商品详情视图")
public record ProductDetailView(
        @Schema(description = "商品ID", example = "1")
        Long id,
        @Schema(description = "商品名称", example = "手测商品A")
        String name,
        @Schema(description = "类目ID", example = "1")
        Long categoryId,
        @Schema(description = "主图", example = "https://example.com/product-a.png")
        String mainImage,
        @Schema(description = "商品相册JSON", example = "[]")
        String albumImages,
        @Schema(description = "商品描述", example = "用于手工测试的商品")
        String description,
        @Schema(description = "默认销售价，单位分", example = "9990")
        Long salePrice,
        @Schema(description = "默认原价，单位分", example = "12990")
        Long originPrice,
        @Schema(description = "销量", example = "100")
        Integer sales,
        @Schema(description = "SKU列表")
        List<ProductSkuView> skus
) {
}
