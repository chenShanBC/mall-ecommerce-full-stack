package com.mallfei.order.domain.model;

import com.mallfei.common.exception.BusinessException;

import java.time.LocalDateTime;

public record OrderRefund(
        Long id,
        Long orderId,
        String orderNo,
        Long userId,
        String refundNo,
        Long refundAmountCent,
        String channelRefundNo,
        String refundStatus,
        String refundReason,
        String failReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static final String STATUS_PENDING = "REFUND_PENDING";
    public static final String STATUS_REFUNDING = "REFUNDING";
    public static final String STATUS_SUCCESS = "REFUND_SUCCESS";
    public static final String STATUS_FAILED = "REFUND_FAILED";
    public static final String STATUS_CLOSED = "REFUND_CLOSED";

    public static OrderRefund create(Long orderId,
                                     String orderNo,
                                     Long userId,
                                     String refundNo,
                                     Long refundAmountCent,
                                     String refundReason) {
        if (refundNo == null || refundNo.isBlank()) {
            throw BusinessException.badRequest("退款单号不能为空");
        }
        if (refundAmountCent == null || refundAmountCent <= 0) {
            throw BusinessException.badRequest("退款金额必须大于0");
        }
        return new OrderRefund(null, orderId, orderNo, userId, refundNo, refundAmountCent, "", STATUS_PENDING,
                refundReason == null ? "" : refundReason, "", null, null);
    }

    public boolean refunding() {
        return STATUS_REFUNDING.equals(refundStatus);
    }

    public boolean success() {
        return STATUS_SUCCESS.equals(refundStatus);
    }

    public boolean failed() {
        return STATUS_FAILED.equals(refundStatus);
    }

    public boolean fullRefund(long orderPayAmountCent) {
        return refundAmountCent != null && refundAmountCent >= orderPayAmountCent;
    }

    public OrderRefund markRefunding() {
        if (success()) {
            return this;
        }
        if (!STATUS_PENDING.equals(refundStatus) && !failed()) {
            throw BusinessException.badRequest("当前退款状态不允许进入退款中");
        }
        return copy(STATUS_REFUNDING, channelRefundNo, "");
    }

    public OrderRefund markSuccess(String nextChannelRefundNo) {
        if (success()) {
            return this;
        }
        if (!refunding() && !STATUS_PENDING.equals(refundStatus)) {
            throw BusinessException.badRequest("当前退款状态不允许标记成功");
        }
        return copy(STATUS_SUCCESS, nextChannelRefundNo == null ? "" : nextChannelRefundNo, "");
    }

    public OrderRefund markFailed(String reason) {
        if (success()) {
            throw BusinessException.badRequest("已退款成功的申请不能标记失败");
        }
        if (failed()) {
            return this;
        }
        return copy(STATUS_FAILED, channelRefundNo, reason == null ? "" : reason);
    }

    private OrderRefund copy(String nextStatus, String nextChannelRefundNo, String nextFailReason) {
        return new OrderRefund(id, orderId, orderNo, userId, refundNo, refundAmountCent,
                nextChannelRefundNo == null ? "" : nextChannelRefundNo, nextStatus, refundReason,
                nextFailReason == null ? "" : nextFailReason, createdAt, LocalDateTime.now());
    }
}
