package com.mallfei.pay.domain.model;

import java.time.LocalDateTime;

public record PayReconciliationRecord(
        Long id,
        String batchNo,
        String bizType,
        String orderNo,
        String payOrderNo,
        String refundNo,
        String localStatus,
        String channelStatus,
        Long localAmountCent,
        Long channelAmountCent,
        Boolean consistent,
        String diffType,
        String repairStatus,
        String remark,
        LocalDateTime repairedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static final String BIZ_TYPE_PAY = "PAY";
    public static final String BIZ_TYPE_REFUND = "REFUND";

    public static final String DIFF_NONE = "NONE";
    public static final String DIFF_LOCAL_MISSING = "LOCAL_MISSING";
    public static final String DIFF_CHANNEL_MISSING = "CHANNEL_MISSING";
    public static final String DIFF_LONG_PLATFORM_SUCCESS_CHANNEL_MISSING = "LONG_PLATFORM_SUCCESS_CHANNEL_MISSING";
    public static final String DIFF_SHORT_CHANNEL_SUCCESS_PLATFORM_UNPAID = "SHORT_CHANNEL_SUCCESS_PLATFORM_UNPAID";
    public static final String DIFF_SHORT_PAID_ORDER_PAY_MISSING = "SHORT_PAID_ORDER_PAY_MISSING";
    public static final String DIFF_ORDER_STATUS_NOT_SYNCED = "ORDER_STATUS_NOT_SYNCED";
    public static final String DIFF_STATUS_MISMATCH = "STATUS_MISMATCH";
    public static final String DIFF_AMOUNT_MISMATCH = "AMOUNT_MISMATCH";
    public static final String DIFF_STATUS_AND_AMOUNT_MISMATCH = "STATUS_AND_AMOUNT_MISMATCH";
    public static final String DIFF_LOCAL_PAYING_CHANNEL_SUCCESS = "LOCAL_PAYING_CHANNEL_SUCCESS";
    public static final String DIFF_PAY_SUCCESS_ORDER_NOT_PAID = "PAY_SUCCESS_ORDER_NOT_PAID";
    public static final String DIFF_UNPAID_ORDER_NEED_CLOSE_RELEASE = "UNPAID_ORDER_NEED_CLOSE_RELEASE";
    public static final String DIFF_UNKNOWN = "UNKNOWN";

    public static final String REPAIR_NONE = "NONE";
    public static final String REPAIR_PENDING = "PENDING";
    public static final String REPAIR_RECHECKING = "RECHECKING";
    public static final String REPAIR_WAIT_CHANNEL_CONFIRM = "WAIT_CHANNEL_CONFIRM";
    public static final String REPAIR_WAIT_USER_REPAY = "WAIT_USER_REPAY";
    public static final String REPAIR_WAIT_REFUND = "WAIT_REFUND";
    public static final String REPAIR_MANUAL_REVIEW = "MANUAL_REVIEW";
    public static final String REPAIR_DONE = "DONE";
    public static final String REPAIR_IGNORED = "IGNORED";

    public static PayReconciliationRecord pay(OrderPayReconciliationSnapshot snapshot) {
        String diffType = resolveDiffType(snapshot.payExists(), snapshot.statusConsistent(), snapshot.amountConsistent());
        return create(
                BIZ_TYPE_PAY,
                snapshot.orderNo(),
                snapshot.payOrderNo(),
                null,
                snapshot.localStatus(),
                snapshot.channelStatus(),
                snapshot.localAmountCent(),
                snapshot.channelAmountCent(),
                DIFF_NONE.equals(diffType),
                diffType,
                snapshot.remark()
        );
    }

    public static PayReconciliationRecord refund(RefundReconciliationSnapshot snapshot) {
        String diffType = resolveRefundDiffType(snapshot.refundExists(), snapshot.channelKnown(), snapshot.statusConsistent(), snapshot.amountConsistent());
        return create(
                BIZ_TYPE_REFUND,
                snapshot.orderNo(),
                snapshot.payOrderNo(),
                snapshot.refundNo(),
                snapshot.localStatus(),
                snapshot.channelStatus(),
                snapshot.localAmountCent(),
                snapshot.channelAmountCent(),
                DIFF_NONE.equals(diffType),
                diffType,
                snapshot.remark()
        );
    }

    public static PayReconciliationRecord paySyncPending(OrderPaySyncPendingSnapshot snapshot) {
        return create(
                BIZ_TYPE_PAY,
                snapshot.orderNo(),
                snapshot.payOrderNo(),
                null,
                snapshot.localStatus(),
                snapshot.channelStatus(),
                snapshot.localAmountCent(),
                snapshot.channelAmountCent(),
                false,
                DIFF_LOCAL_PAYING_CHANNEL_SUCCESS,
                snapshot.remark()
        );
    }

    public static PayReconciliationRecord orderRepairPending(OrderRepairPendingSnapshot snapshot) {
        return create(
                BIZ_TYPE_PAY,
                snapshot.orderNo(),
                snapshot.payOrderNo(),
                null,
                snapshot.localStatus(),
                snapshot.channelStatus(),
                snapshot.localAmountCent(),
                snapshot.channelAmountCent(),
                false,
                DIFF_PAY_SUCCESS_ORDER_NOT_PAID,
                snapshot.remark()
        );
    }

    public static PayReconciliationRecord suspicious(OrderRepairPendingSnapshot snapshot, String diffType) {
        return create(
                BIZ_TYPE_PAY,
                snapshot.orderNo(),
                snapshot.payOrderNo(),
                null,
                snapshot.localStatus(),
                snapshot.channelStatus(),
                snapshot.localAmountCent(),
                snapshot.channelAmountCent(),
                false,
                require(diffType, "diffType"),
                snapshot.remark()
        );
    }

    public static PayReconciliationRecord closeReleasePending(OrderRepairPendingSnapshot snapshot) {
        return create(
                BIZ_TYPE_PAY,
                snapshot.orderNo(),
                snapshot.payOrderNo(),
                null,
                snapshot.localStatus(),
                snapshot.channelStatus(),
                snapshot.localAmountCent(),
                snapshot.channelAmountCent(),
                false,
                DIFF_UNPAID_ORDER_NEED_CLOSE_RELEASE,
                snapshot.remark()
        );
    }

    private static PayReconciliationRecord create(String bizType,
                                                  String orderNo,
                                                  String payOrderNo,
                                                  String refundNo,
                                                  String localStatus,
                                                  String channelStatus,
                                                  Long localAmountCent,
                                                  Long channelAmountCent,
                                                  boolean consistent,
                                                  String diffType,
                                                  String remark) {
        LocalDateTime now = LocalDateTime.now();
        return new PayReconciliationRecord(
                null,
                newBatchNo(bizType),
                bizType,
                require(orderNo, "orderNo"),
                blankToNull(payOrderNo),
                blankToNull(refundNo),
                blankToNull(localStatus),
                blankToNull(channelStatus),
                localAmountCent,
                channelAmountCent,
                consistent,
                diffType,
                consistent ? REPAIR_NONE : REPAIR_PENDING,
                truncate(remark, 512),
                null,
                now,
                now
        );
    }

    public PayReconciliationRecord markRepairDone(String nextRemark) {
        return markRepair(REPAIR_DONE, nextRemark);
    }

    public PayReconciliationRecord markIgnored(String nextRemark) {
        return markRepair(REPAIR_IGNORED, nextRemark);
    }

    public PayReconciliationRecord markHandlingStatus(String nextRepairStatus, String nextRemark) {
        return markRepair(require(nextRepairStatus, "repairStatus").trim().toUpperCase(), nextRemark);
    }

    public PayReconciliationRecord refreshFrom(PayReconciliationRecord latest) {
        return new PayReconciliationRecord(
                id,
                latest.batchNo(),
                latest.bizType(),
                latest.orderNo(),
                latest.payOrderNo(),
                latest.refundNo(),
                latest.localStatus(),
                latest.channelStatus(),
                latest.localAmountCent(),
                latest.channelAmountCent(),
                latest.consistent(),
                latest.diffType(),
                repairStatus,
                mergeRemark(remark, latest.remark()),
                repairedAt,
                createdAt,
                LocalDateTime.now()
        );
    }

    private String mergeRemark(String oldRemark, String latestRemark) {
        if (latestRemark == null || latestRemark.isBlank()) {
            return oldRemark;
        }
        if (oldRemark == null || oldRemark.isBlank()) {
            return truncate(latestRemark, 512);
        }
        return truncate(latestRemark + "；最近刷新前备注=" + oldRemark, 512);
    }

    private PayReconciliationRecord markRepair(String nextRepairStatus, String nextRemark) {
        LocalDateTime now = LocalDateTime.now();
        return new PayReconciliationRecord(id, batchNo, bizType, orderNo, payOrderNo, refundNo, localStatus, channelStatus,
                localAmountCent, channelAmountCent, consistent, diffType, nextRepairStatus,
                truncate(nextRemark == null || nextRemark.isBlank() ? remark : nextRemark, 512), now, createdAt, now);
    }

    public PayReconciliationRecord withBatchNo(String nextBatchNo) {
        if (nextBatchNo == null || nextBatchNo.isBlank()) {
            return this;
        }
        return new PayReconciliationRecord(id, nextBatchNo.trim(), bizType, orderNo, payOrderNo, refundNo, localStatus, channelStatus,
                localAmountCent, channelAmountCent, consistent, diffType, repairStatus, remark, repairedAt, createdAt, updatedAt);
    }

    private static String resolveDiffType(boolean localExists, boolean statusConsistent, boolean amountConsistent) {
        if (!localExists) {
            return DIFF_LOCAL_MISSING;
        }
        if (statusConsistent && amountConsistent) {
            return DIFF_NONE;
        }
        if (!statusConsistent && !amountConsistent) {
            return DIFF_STATUS_AND_AMOUNT_MISMATCH;
        }
        if (!statusConsistent) {
            return DIFF_STATUS_MISMATCH;
        }
        return DIFF_AMOUNT_MISMATCH;
    }

    private static String resolveRefundDiffType(boolean refundExists, boolean channelKnown, boolean statusConsistent, boolean amountConsistent) {
        if (!refundExists) {
            return DIFF_LOCAL_MISSING;
        }
        if (!channelKnown) {
            return DIFF_CHANNEL_MISSING;
        }
        if (statusConsistent && amountConsistent) {
            return DIFF_NONE;
        }
        if (!statusConsistent && !amountConsistent) {
            return DIFF_STATUS_AND_AMOUNT_MISMATCH;
        }
        if (!statusConsistent) {
            return DIFF_STATUS_MISMATCH;
        }
        return DIFF_AMOUNT_MISMATCH;
    }

    private static String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String newBatchNo(String bizType) {
        return "RC" + bizType + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record OrderPayReconciliationSnapshot(
            String orderNo,
            String payOrderNo,
            String localStatus,
            String channelStatus,
            Long localAmountCent,
            Long channelAmountCent,
            boolean payExists,
            boolean statusConsistent,
            boolean amountConsistent,
            String remark
    ) {
    }

    public record RefundReconciliationSnapshot(
            String orderNo,
            String payOrderNo,
            String refundNo,
            String localStatus,
            String channelStatus,
            Long localAmountCent,
            Long channelAmountCent,
            boolean refundExists,
            boolean channelKnown,
            boolean statusConsistent,
            boolean amountConsistent,
            String remark
    ) {
    }

    public record OrderPaySyncPendingSnapshot(
            String orderNo,
            String payOrderNo,
            String localStatus,
            String channelStatus,
            Long localAmountCent,
            Long channelAmountCent,
            String remark
    ) {
    }

    public record OrderRepairPendingSnapshot(
            String orderNo,
            String payOrderNo,
            String localStatus,
            String channelStatus,
            Long localAmountCent,
            Long channelAmountCent,
            String remark
    ) {
    }
}
