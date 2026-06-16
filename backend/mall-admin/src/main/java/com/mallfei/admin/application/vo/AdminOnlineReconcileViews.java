package com.mallfei.admin.application.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AdminOnlineReconcileViews {
    private AdminOnlineReconcileViews() {}

    public record TaskView(Long id, String taskNo, LocalDate reconcileDate, String channel, String status,
                           String localBillStatus, String channelBillStatus, String matchStatus,
                           Long localTotalCount, Long localTotalAmountCent, Long channelTotalCount, Long channelTotalAmountCent,
                           Long matchedCount, Long diffCount, Long pendingCount, Long doneCount, Long hangCount,
                           String remark, LocalDateTime createdAt, LocalDateTime matchedAt, LocalDateTime completedAt) {}

    public record LocalBillItemView(Long id, Long taskId, String bizType, String orderNo, String payOrderNo,
                                    String refundNo, Long userId, String localStatus, String orderStatus,
                                    Long amountCent, String channel, String transactionNo, LocalDateTime tradeTime) {}

    public record ChannelBillItemView(Long id, Long taskId, String bizType, String channel, String outTradeNo,
                                      String orderNo, String payOrderNo, String refundNo, String channelTradeNo,
                                      String channelRefundNo, String channelStatus, Long amountCent, Long feeCent,
                                      LocalDateTime tradeTime) {}

    public record DiffItemView(Long id, Long taskId, String bizType, String diffType, String diffLevel,
                               String orderNo, String payOrderNo, String refundNo, Long localItemId, Long channelItemId,
                               String localStatus, String channelStatus, Long localAmountCent, Long channelAmountCent,
                               Long diffAmountCent, String suggestedAction, String processStatus, String processRemark,
                               String processedBy, LocalDateTime processedAt, LocalDateTime createdAt) {}

    public record DiffDetailView(DiffItemView diff, LocalBillItemView localBill, ChannelBillItemView channelBill,
                                 java.util.List<OperationLogView> logs) {}

    public record OperationLogView(Long id, Long taskId, Long diffItemId, String operationType,
                                   String operationContent, String operatorName, String remark, LocalDateTime createdAt) {}

    public record HangingFollowView(Long diffId, Long taskId, String taskNo, LocalDate reconcileDate, String channel,
                                    String bizType, String diffType, String orderNo, String payOrderNo, String refundNo,
                                    Long diffAmountCent, String processStatus, String processRemark, String processedBy,
                                    LocalDateTime hangingAt, LocalDateTime latestFollowAt, String latestFollowContent,
                                    Long hangingDays, String riskLevel) {}

    public record ArchiveReportView(Long totalTasks, Long completedTasks, Long totalLocalCount, Long totalChannelCount,
                                    Long totalMatchedCount, Long totalDiffCount, Long totalPendingCount, Long totalHangCount,
                                    Long localTotalAmountCent, Long channelTotalAmountCent, Long netDiffAmountCent,
                                    java.util.List<DiffTypeStatView> diffTypeStats,
                                    java.util.List<TaskView> tasks) {}

    public record DiffTypeStatView(String diffType, Long count, Long amountCent) {}
}
