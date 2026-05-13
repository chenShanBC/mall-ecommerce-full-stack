package com.mallfei.product.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminProductSkuEditView", description = "后台商品SKU编辑视图")
public record AdminProductSkuEditView(
        @Schema(description = "SKU ID", example = "12")
        Long id,
        @Schema(description = "SKU编码", example = "TEST-SKU-001")
        String skuCode,
        @Schema(description = "SKU名称", example = "手测商品A-默认规格")
        String skuName,
        @Schema(description = "规格JSON", example = "{\"color\":\"black\"}")
        String specJson,
        @Schema(description = "销售价，单位分", example = "9990")
        Long salePriceCent,
        @Schema(description = "原价，单位分", example = "12990")
        Long originPriceCent,
        @Schema(description = "SKU状态", example = "ONLINE")
        String status,
        @Schema(description = "销量", example = "100")
        Integer salesCount,
        @Schema(description = "当前总库存", example = "120")
        Integer totalStock,
        @Schema(description = "当前可用库存", example = "100")
        Integer availableStock,
        @Schema(description = "当前锁定库存", example = "20")
        Integer lockedStock,
        @Schema(description = "库存预警状态", example = "LOW")
        String warningStatus
) {
}
