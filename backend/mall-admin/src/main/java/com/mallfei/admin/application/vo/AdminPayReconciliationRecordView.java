package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "AdminPayReconciliationRecordView", description = "后台支付/退款对账记录视图")
public record AdminPayReconciliationRecordView(
        @Schema(description = "记录ID", example = "1")
        Long id,
        @Schema(description = "对账批次号", example = "RCPAY20260607120000123")
        String batchNo,
        @Schema(description = "业务类型", example = "PAY")
        String bizType,
        @Schema(description = "订单号", example = "ORD202605010001")
        String orderNo,
        @Schema(description = "支付单号", example = "PAY202605010001")
        String payOrderNo,
        @Schema(description = "退款单号", example = "ORF202605010001")
        String refundNo,
        @Schema(description = "本地状态", example = "SUCCESS")
        String localStatus,
        @Schema(description = "渠道状态", example = "TRADE_SUCCESS")
        String channelStatus,
        @Schema(description = "本地金额，单位分", example = "9990")
        Long localAmountCent,
        @Schema(description = "渠道金额，单位分", example = "9990")
        Long channelAmountCent,
        @Schema(description = "是否一致", example = "true")
        Boolean consistent,
        @Schema(description = "差异类型", example = "STATUS_MISMATCH")
        String diffType,
        @Schema(description = "修复状态", example = "PENDING")
        String repairStatus,
        @Schema(description = "备注")
        String remark,
        @Schema(description = "差异分类", example = "STATUS")
        String diffCategory,
        @Schema(description = "差异分类名称", example = "状态不一致")
        String diffCategoryLabel,
        @Schema(description = "处理建议")
        String handleSuggestion,
        @Schema(description = "可执行处置动作")
        List<AdminReconciliationActionView> availableActions,
        @Schema(description = "修复时间")
        LocalDateTime repairedAt,
        @Schema(description = "创建时间")
        LocalDateTime createdAt
) {
}
