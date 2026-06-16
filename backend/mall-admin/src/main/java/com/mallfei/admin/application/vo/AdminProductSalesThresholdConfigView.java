package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminProductSalesThresholdConfigView", description = "商品销售表现阈值全局配置")
public record AdminProductSalesThresholdConfigView(
        @Schema(description = "热销阈值，近30天销量大于等于该值判定为热销", example = "100")
        Integer hotSalesThreshold,
        @Schema(description = "低销阈值，近30天销量小于等于该值判定为低销", example = "10")
        Integer lowSalesThreshold
) {
}
