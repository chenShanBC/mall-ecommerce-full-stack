package com.mallfei.product.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CategoryAdminView", description = "后台类目视图")
public record CategoryAdminView(
        @Schema(description = "类目ID", example = "1")
        Long id,
        @Schema(description = "类目名称", example = "手机数码")
        String name,
        @Schema(description = "父类目ID", example = "0")
        Long parentId,
        @Schema(description = "层级", example = "1")
        Integer level,
        @Schema(description = "排序值", example = "10")
        Integer sortOrder,
        @Schema(description = "状态", example = "ENABLED")
        String status
) {
}
