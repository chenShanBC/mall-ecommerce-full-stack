package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminTodayActiveStockView", description = "后台仓储看板近3日有库存日志的SKU库存明细")
public record AdminTodayActiveStockView(
        @Schema(description = "SKU ID", example = "1001")
        Long skuId,
        @Schema(description = "SKU名称", example = "iPhone 15 Pro 黑色 256G")
        String skuName,
        @Schema(description = "SPU ID", example = "10")
        Long spuId,
        @Schema(description = "商品分类ID", example = "1")
        Long categoryId,
        @Schema(description = "商品分类名称", example = "手机数码")
        String categoryName,
        @Schema(description = "商品类型名称", example = "手机")
        String productTypeName,
        @Schema(description = "总库存", example = "120")
        Integer totalStock,
        @Schema(description = "锁定库存", example = "18")
        Integer lockedStock,
        @Schema(description = "可用库存", example = "102")
        Integer availableStock,
        @Schema(description = "库存状态", example = "ACTIVE")
        String stockStatus,
        @Schema(description = "低库存阈值", example = "20")
        Integer lowStockThreshold,
        @Schema(description = "高库存阈值", example = "200")
        Integer highStockThreshold,
        @Schema(description = "库存预警状态", example = "NORMAL")
        String warningStatus,
        @Schema(description = "最近库存日志时间", example = "2026-06-18T14:22:00")
        String latestStockTime,
        @Schema(description = "数据来源", example = "TODAY_STOCK_LOG")
        String source
) {
}
