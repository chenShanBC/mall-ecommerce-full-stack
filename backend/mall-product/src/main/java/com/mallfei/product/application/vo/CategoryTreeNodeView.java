package com.mallfei.product.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CategoryTreeNodeView", description = "类目树节点")
public record CategoryTreeNodeView(
        @Schema(description = "类目ID", example = "1")
        Long id,
        @Schema(description = "类目名称", example = "手机数码")
        String name,
        @Schema(description = "子类目列表")
        java.util.List<CategoryTreeNodeView> children
) {
}
