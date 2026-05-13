package com.mallfei.aftersale.domain.model;

import com.mallfei.common.exception.BusinessException;

import java.time.LocalDateTime;

public record AftersaleOrder(
        Long id,
        String aftersaleNo,
        String orderNo,
        Long userId,
        String aftersaleType,
        String status,
        Long refundAmountCent,
        String reason,
        Integer version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static final String TYPE_ONLY_REFUND = "ONLY_REFUND";
    public static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_REFUND_PROCESSING = "REFUND_PROCESSING";
    public static final String STATUS_REFUND_SUCCESS = "REFUND_SUCCESS";
    public static final String STATUS_REFUND_FAILED = "REFUND_FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static AftersaleOrder createOnlyRefund(String aftersaleNo, String orderNo, Long userId, Long refundAmountCent, String reason, LocalDateTime now) {
        return new AftersaleOrder(null, aftersaleNo, orderNo, userId, TYPE_ONLY_REFUND, STATUS_PENDING_REVIEW, refundAmountCent, reason, 0, now, now);
    }

    public AftersaleOrder approve(LocalDateTime now) {
        ensureStatus(STATUS_PENDING_REVIEW, "当前售后单状态不允许审核通过");
        return new AftersaleOrder(id, aftersaleNo, orderNo, userId, aftersaleType, STATUS_APPROVED, refundAmountCent, reason, nextVersion(), createdAt, now);
    }

    public AftersaleOrder reject(LocalDateTime now) {
        ensureStatus(STATUS_PENDING_REVIEW, "当前售后单状态不允许驳回");
        return new AftersaleOrder(id, aftersaleNo, orderNo, userId, aftersaleType, STATUS_REJECTED, refundAmountCent, reason, nextVersion(), createdAt, now);
    }

    private void ensureStatus(String expected, String message) {
        if (!expected.equals(status)) {
            throw BusinessException.badRequest(message);
        }
    }

    private int nextVersion() {
        return version == null ? 0 : version + 1;
    }
}
