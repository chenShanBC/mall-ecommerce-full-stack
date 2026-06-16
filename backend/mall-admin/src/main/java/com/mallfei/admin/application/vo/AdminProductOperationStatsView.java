package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminProductOperationStatsView", description = "商品运营统计视图")
public record AdminProductOperationStatsView(
        @Schema(description = "商品总数", example = "120")
        long totalCount,
        @Schema(description = "上架商品数", example = "96")
        long onlineCount,
        @Schema(description = "待上架商品数", example = "24")
        long pendingOnlineCount,
        @Schema(description = "热销商品数，取累计销量最高的前 5 个有销量商品", example = "5")
        long hotSellingCount,
        @Schema(description = "低销商品数，按上架且近30天销量小于低销阈值统计", example = "12")
        long lowSellingCount,
        @Schema(description = "近7天完成销量", example = "86")
        long recent7DaySalesCount,
        @Schema(description = "近30天完成销量", example = "320")
        long recent30DaySalesCount,
        @Schema(description = "当前自然月完成销量", example = "128")
        long currentMonthSalesCount,
        @Schema(description = "近30天完成销售额，单位分", example = "1299000")
        long recent30DaySalesAmountCent
) {
}
