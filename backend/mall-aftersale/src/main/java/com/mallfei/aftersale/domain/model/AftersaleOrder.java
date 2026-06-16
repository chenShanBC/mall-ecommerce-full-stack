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
        String originOrderStatus,
        Long refundAmountCent,
        String reason,
        String rejectReason,
        String refundNo,
        String failReason,
        Integer version,
        LocalDateTime reviewedAt,
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

    public static AftersaleOrder createOnlyRefund(String aftersaleNo, String orderNo, Long userId, String originOrderStatus, Long refundAmountCent, String reason, LocalDateTime now) {
        return new AftersaleOrder(null, aftersaleNo, orderNo, userId, TYPE_ONLY_REFUND, STATUS_PENDING_REVIEW, originOrderStatus, refundAmountCent, reason, null, null, null, 0, null, now, now);
    }

    public AftersaleOrder approve(LocalDateTime now) {
        ensureStatus(STATUS_PENDING_REVIEW, "当前售后单状态不允许审核通过");
        return new AftersaleOrder(id, aftersaleNo, orderNo, userId, aftersaleType, STATUS_APPROVED, originOrderStatus, refundAmountCent, reason, rejectReason, refundNo, failReason, nextVersion(), now, createdAt, now);
    }

    public AftersaleOrder reject(String reason, LocalDateTime now) {
        ensureStatus(STATUS_PENDING_REVIEW, "当前售后单状态不允许驳回");
        return new AftersaleOrder(id, aftersaleNo, orderNo, userId, aftersaleType, STATUS_REJECTED, originOrderStatus, refundAmountCent, this.reason, reason, refundNo, failReason, nextVersion(), now, createdAt, now);
    }

    public AftersaleOrder bindRefundNo(String refundNo, LocalDateTime now) {
        return new AftersaleOrder(id, aftersaleNo, orderNo, userId, aftersaleType, STATUS_REFUND_PROCESSING, originOrderStatus, refundAmountCent, reason, rejectReason, refundNo, failReason, nextVersion(), reviewedAt, createdAt, now);
    }

    public AftersaleOrder markRefundSuccess(LocalDateTime now) {
        ensureStatus(STATUS_REFUND_PROCESSING, "当前售后单状态不允许标记退款成功");
        return new AftersaleOrder(id, aftersaleNo, orderNo, userId, aftersaleType, STATUS_REFUND_SUCCESS, originOrderStatus, refundAmountCent, reason, rejectReason, refundNo, null, nextVersion(), reviewedAt, createdAt, now);
    }

    public AftersaleOrder markRefundFailed(String failReason, LocalDateTime now) {
        ensureStatus(STATUS_REFUND_PROCESSING, "当前售后单状态不允许标记退款失败");
        return new AftersaleOrder(id, aftersaleNo, orderNo, userId, aftersaleType, STATUS_REFUND_FAILED, originOrderStatus, refundAmountCent, reason, rejectReason, refundNo, failReason, nextVersion(), reviewedAt, createdAt, now);
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
