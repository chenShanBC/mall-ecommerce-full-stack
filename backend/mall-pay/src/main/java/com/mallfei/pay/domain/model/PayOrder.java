package com.mallfei.pay.domain.model;

import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.domain.model.Order;

import java.time.LocalDateTime;

public record PayOrder(
        Long id,
        String payOrderNo,
        String orderNo,
        Long userId,
        Long payAmountCent,
        String payStatus,
        String payChannel,
        String transactionNo,
        String idempotentKey,
        Integer version,
        String callbackPayload
) {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAYING = "PAYING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_REFUND_PENDING = "REFUND_PENDING";
    public static final String STATUS_REFUNDING = "REFUNDING";
    public static final String STATUS_REFUNDED = "REFUNDED";
    public static final String STATUS_PARTIALLY_REFUNDED = "PARTIALLY_REFUNDED";
    public static final String STATUS_REFUND_FAILED = "REFUND_FAILED";
    public static final String CHANNEL_MOCK = "MOCK";
    public static final String CHANNEL_ALIPAY_WAP = "ALIPAY_WAP";
    public static final String CHANNEL_ALIPAY_PC = "ALIPAY_PC";
    public static final String CHANNEL_WECHAT_H5 = "WECHAT_H5";

    public static PayOrder createPending(String payOrderNo, String orderNo, Long userId, Long payAmountCent, String payChannel) {
        String resolvedChannel = payChannel == null || payChannel.isBlank() ? CHANNEL_MOCK : payChannel;
        return new PayOrder(null, payOrderNo, orderNo, userId, payAmountCent, STATUS_PENDING, resolvedChannel, "", "PAY:" + resolvedChannel + ":" + orderNo, 0, "");
    }

    public boolean pending() {
        return STATUS_PENDING.equals(payStatus) || STATUS_PAYING.equals(payStatus);
    }

    public boolean success() {
        return STATUS_SUCCESS.equals(payStatus);
    }

    public boolean closed() {
        return STATUS_CLOSED.equals(payStatus);
    }

    public boolean refunded() {
        return STATUS_REFUNDED.equals(payStatus) || STATUS_PARTIALLY_REFUNDED.equals(payStatus);
    }

    public boolean sameChannel(String targetChannel) {
        String resolvedChannel = targetChannel == null || targetChannel.isBlank() ? CHANNEL_MOCK : targetChannel;
        return resolvedChannel.equals(payChannel);
    }

    public boolean reusableFor(Order order) {
        return !pending() || order.pendingPayment();
    }

    public boolean canCallbackSuccessFor(Order order) {
        if (success()) {
            return true;
        }
        return pending() && order.pendingPayment();
    }

    public boolean shouldEscalateCallbackFor(Order order) {
        return pending() && order.paymentException();
    }

    public boolean refundable() {
        return success() || STATUS_PARTIALLY_REFUNDED.equals(payStatus) || STATUS_REFUND_PENDING.equals(payStatus) || STATUS_REFUND_FAILED.equals(payStatus);
    }

    public PayOrder withSubmission(String submitPayload) {
        return new PayOrder(id, payOrderNo, orderNo, userId, payAmountCent, payStatus, payChannel, transactionNo, idempotentKey, nextVersion(), submitPayload == null ? "" : submitPayload);
    }

    public PayOrder markSuccess(String transactionNo, LocalDateTime successTime) {
        if (success()) {
            return this;
        }
        if (!pending()) {
            throw BusinessException.badRequest("当前支付单状态不允许回调成功");
        }
        String payload = "{\"orderNo\":\"" + orderNo + "\",\"channel\":\"" + payChannel + "\",\"successTime\":\"" + successTime + "\"}";
        return new PayOrder(id, payOrderNo, orderNo, userId, payAmountCent, STATUS_SUCCESS, payChannel, transactionNo, idempotentKey, nextVersion(), payload);
    }

    public PayOrder markRefundPending(LocalDateTime now, String reason) {
        if (!refundable()) {
            throw BusinessException.badRequest("当前支付单状态不允许发起退款");
        }
        String payload = "{\"refundPendingAt\":\"" + now + "\",\"reason\":\"" + reason + "\"}";
        return new PayOrder(id, payOrderNo, orderNo, userId, payAmountCent, STATUS_REFUND_PENDING, payChannel, transactionNo, idempotentKey, nextVersion(), payload);
    }

    public PayOrder markRefunding(LocalDateTime now, String reason) {
        if (STATUS_REFUNDING.equals(payStatus)) {
            return this;
        }
        if (!STATUS_REFUND_PENDING.equals(payStatus)) {
            throw BusinessException.badRequest("当前支付单状态不允许进入退款处理中");
        }
        String payload = "{\"refundingAt\":\"" + now + "\",\"reason\":\"" + reason + "\"}";
        return new PayOrder(id, payOrderNo, orderNo, userId, payAmountCent, STATUS_REFUNDING, payChannel, transactionNo, idempotentKey, nextVersion(), payload);
    }

    public PayOrder markRefundSuccess(LocalDateTime now, String reason) {
        return markRefundSuccess(now, reason, payAmountCent);
    }

    public PayOrder markRefundSuccess(LocalDateTime now, String reason, Long refundAmountCent) {
        if (STATUS_REFUNDED.equals(payStatus)) {
            return this;
        }
        if (!refundable() && !STATUS_REFUNDING.equals(payStatus)) {
            throw BusinessException.badRequest("当前支付单状态不允许退款成功");
        }
        boolean fullRefund = refundAmountCent == null || payAmountCent == null || refundAmountCent >= payAmountCent;
        String nextStatus = fullRefund ? STATUS_REFUNDED : STATUS_PARTIALLY_REFUNDED;
        String payload = "{\"refundSuccessAt\":\"" + now + "\",\"reason\":\"" + reason + "\",\"refundAmountCent\":" + refundAmountCent + "}";
        return new PayOrder(id, payOrderNo, orderNo, userId, payAmountCent, nextStatus, payChannel, transactionNo, idempotentKey, nextVersion(), payload);
    }

    public PayOrder markRefundFailed(LocalDateTime now, String reason) {
        if (!STATUS_REFUND_PENDING.equals(payStatus) && !STATUS_REFUNDING.equals(payStatus)) {
            throw BusinessException.badRequest("当前支付单状态不允许标记退款失败");
        }
        String payload = "{\"refundFailedAt\":\"" + now + "\",\"reason\":\"" + reason + "\"}";
        return new PayOrder(id, payOrderNo, orderNo, userId, payAmountCent, STATUS_REFUND_FAILED, payChannel, transactionNo, idempotentKey, nextVersion(), payload);
    }

    public PayOrder close(String reason) {
        if (success() || refunded()) {
            throw BusinessException.badRequest("已支付成功或已退款的支付单不能关闭");
        }
        if (closed()) {
            return this;
        }
        return new PayOrder(id, payOrderNo, orderNo, userId, payAmountCent, STATUS_CLOSED, payChannel, transactionNo, idempotentKey, nextVersion(), "{\"closedBy\":\"" + reason + "\"}");
    }

    public PayOrder closeByOrderStatus(String reasonStatus) {
        if (!pending()) {
            return this;
        }
        return new PayOrder(id, payOrderNo, orderNo, userId, payAmountCent, STATUS_CLOSED, payChannel, transactionNo, idempotentKey, nextVersion(), "{\"closedByOrderStatus\":\"" + reasonStatus + "\"}");
    }

    public boolean amountConsistentWith(Long amountCent) {
        return String.valueOf(amountCent).equals(String.valueOf(payAmountCent));
    }

    private int nextVersion() {
        return version == null ? 0 : version + 1;
    }
}
