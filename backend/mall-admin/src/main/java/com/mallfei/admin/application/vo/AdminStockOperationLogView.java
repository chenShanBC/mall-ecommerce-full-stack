package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminStockOperationLogView", description = "后台库存操作日志视图")
public record AdminStockOperationLogView(
        @Schema(description = "日志ID", example = "1")
        Long id,
        @Schema(description = "SKU ID", example = "1001")
        Long skuId,
        @Schema(description = "操作类型", example = "MANUAL_ADJUST")
        String operationType,
        @Schema(description = "业务类型", example = "ADMIN")
        String businessType,
        @Schema(description = "业务单号", example = "1001")
        String businessNo,
        @Schema(description = "变更数量", example = "10")
        Integer changeQuantity,
        @Schema(description = "变更前总库存", example = "100")
        Integer beforeTotalStock,
        @Schema(description = "变更前锁定库存", example = "0")
        Integer beforeLockedStock,
        @Schema(description = "变更前可用库存", example = "100")
        Integer beforeAvailableStock,
        @Schema(description = "变更后总库存", example = "110")
        Integer afterTotalStock,
        @Schema(description = "变更后锁定库存", example = "0")
        Integer afterLockedStock,
        @Schema(description = "变更后可用库存", example = "110")
        Integer afterAvailableStock,
        @Schema(description = "备注", example = "后台手工调整库存")
        String remark,
        @Schema(description = "操作人类型", example = "ADMIN")
        String operatorType,
        @Schema(description = "操作人ID", example = "1")
        Long operatorId,
        @Schema(description = "操作人名称", example = "admin")
        String operatorName,
        @Schema(description = "来源类型", example = "ADMIN_UI")
        String sourceType,
        @Schema(description = "创建时间", example = "2026-05-08T12:30:00")
        String createdAt
) {
}
