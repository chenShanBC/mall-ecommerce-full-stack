package com.mallfei.product.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "AdminProductDetailView", description = "后台商品详情视图")
public record AdminProductDetailView(
        @Schema(description = "商品ID", example = "1")
        Long id,
        @Schema(description = "商品名称", example = "手测商品A")
        String name,
        @Schema(description = "类目ID", example = "1")
        Long categoryId,
        @Schema(description = "主图地址", example = "https://example.com/product-a.png")
        String mainImageUrl,
        @Schema(description = "商品描述", example = "用于手工测试的商品")
        String description,
        @Schema(description = "商品状态", example = "ONLINE")
        String status,
        @Schema(description = "SKU列表")
        List<AdminProductSkuEditView> skus
) {
}
