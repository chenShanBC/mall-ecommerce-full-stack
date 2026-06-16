package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminReconciliationOverviewView", description = "后台对账一期增强概览")
public record AdminReconciliationOverviewView(
        @Schema(description = "支付侧当前可核对订单数", example = "128")
        long paymentTotalCount,
        @Schema(description = "支付侧对平订单数", example = "120")
        long paymentConsistentCount,
        @Schema(description = "支付侧异常订单数", example = "8")
        long paymentAbnormalCount,
        @Schema(description = "历史对账记录数", example = "256")
        long recordTotalCount,
        @Schema(description = "历史正常记录数", example = "240")
        long recordConsistentCount,
        @Schema(description = "历史异常记录数", example = "16")
        long recordAbnormalCount,
        @Schema(description = "待处理异常数", example = "5")
        long pendingHandleCount,
        @Schema(description = "已处理异常数", example = "9")
        long handledCount,
        @Schema(description = "已忽略异常数", example = "2")
        long ignoredCount
) {
}
