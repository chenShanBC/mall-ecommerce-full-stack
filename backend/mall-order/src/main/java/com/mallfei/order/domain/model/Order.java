package com.mallfei.order.domain.model;

import com.mallfei.common.exception.BusinessException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public record Order(
        Long id,
        String orderNo,
        Long userId,
        String orderStatus,
        Long totalAmountCent,
        Long payAmountCent,
        Long freightAmountCent,
        Long discountAmountCent,
        String receiverName,
        String receiverPhone,
        String receiverProvinceName,
        String receiverCityName,
        String receiverDistrictName,
        String receiverDetailAddress,
        String remark,
        String payType,
        LocalDateTime paidAt,
        LocalDateTime cancelledAt,
        LocalDateTime shippedAt,
        LocalDateTime completedAt,
        LocalDateTime expireTime,
        LocalDateTime createdAt,
        Integer version,
        List<OrderItem> items
) {
    public static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SHIPPED = "SHIPPED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_TIMEOUT_CANCELLED = "TIMEOUT_CANCELLED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_REFUND_PENDING = "REFUND_PENDING";
    public static final String STATUS_REFUNDED = "REFUNDED";
    public static final String STATUS_REFUND_CLOSED = "REFUND_CLOSED";
    public static final String STATUS_PARTIALLY_REFUNDED = "PARTIALLY_REFUNDED";
    public static final String STATUS_PAYMENT_EXCEPTION = "PAYMENT_EXCEPTION";

    public static Order createPending(String orderNo, Long userId, Long totalAmountCent, String receiverName, String receiverPhone, String receiverProvinceName, String receiverCityName, String receiverDistrictName, String receiverDetailAddress, String remark, LocalDateTime expireTime, List<OrderItem> items) {
        return new Order(null, orderNo, userId, STATUS_PENDING_PAYMENT, totalAmountCent, totalAmountCent, 0L, 0L, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark == null ? "" : remark, "MOCK", null, null, null, null, expireTime, LocalDateTime.now(), 0, items);
    }

    public boolean belongsTo(Long targetUserId) { return userId != null && userId.equals(targetUserId); }
    public boolean pendingPayment() { return STATUS_PENDING_PAYMENT.equals(orderStatus); }
    public boolean paymentException() { return STATUS_PAYMENT_EXCEPTION.equals(orderStatus); }
    public boolean paid() { return STATUS_PAID.equals(orderStatus) || STATUS_PROCESSING.equals(orderStatus); }
    public boolean processing() { return STATUS_PROCESSING.equals(orderStatus); }
    public boolean shipped() { return STATUS_SHIPPED.equals(orderStatus); }
    public boolean completed() { return STATUS_COMPLETED.equals(orderStatus); }
    public boolean refunded() { return STATUS_REFUNDED.equals(orderStatus) || STATUS_PARTIALLY_REFUNDED.equals(orderStatus); }
    public boolean paidOrAfter() { return STATUS_PAID.equals(orderStatus) || STATUS_PROCESSING.equals(orderStatus) || STATUS_SHIPPED.equals(orderStatus) || STATUS_COMPLETED.equals(orderStatus) || STATUS_REFUND_PENDING.equals(orderStatus) || STATUS_REFUNDED.equals(orderStatus) || STATUS_PARTIALLY_REFUNDED.equals(orderStatus); }
    public boolean cancelled() { return STATUS_CANCELLED.equals(orderStatus) || STATUS_TIMEOUT_CANCELLED.equals(orderStatus) || STATUS_CLOSED.equals(orderStatus) || STATUS_REFUNDED.equals(orderStatus); }
    public boolean timeoutCancelled() { return STATUS_TIMEOUT_CANCELLED.equals(orderStatus); }
    public boolean refundPending() { return STATUS_REFUND_PENDING.equals(orderStatus); }
    public boolean refundable() { return paid() || shipped() || completed() || refundPending() || STATUS_PARTIALLY_REFUNDED.equals(orderStatus); }

    public void ensureRefundable() {
        if (!refundable()) throw BusinessException.badRequest("当前订单状态不允许申请退款");
    }

    public boolean timedOut(LocalDateTime now, long timeoutMinutes) { return timedOut(now); }
    public boolean timedOut(LocalDateTime now) { if (!pendingPayment()) return false; return !now.isBefore(effectiveExpireTime(0)); }
    public LocalDateTime effectiveExpireTime(long fallbackTimeoutMinutes) { return expireTime == null ? createdAtFromOrderNo().plusMinutes(fallbackTimeoutMinutes) : expireTime; }
    public long remainingPaySeconds(LocalDateTime now) { if (!pendingPayment()) return 0L; return Math.max(0L, java.time.Duration.between(now, effectiveExpireTime(0)).getSeconds()); }
    public LocalDateTime createdAtFromOrderNo() { try { long timestamp = Long.parseLong(orderNo.substring(3, 16)); return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()); } catch (Exception ignored) { return LocalDateTime.now(); } }
    public Order cancelByUser(LocalDateTime now) { ensurePendingPayment("当前订单状态不允许取消"); return copy(STATUS_CANCELLED, paidAt, now, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order cancelByAdmin(LocalDateTime now) { ensureAdminClosable(); return copy(STATUS_CANCELLED, paidAt, now, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order cancelByTimeout(LocalDateTime now) { if (!pendingPayment()) return this; return copy(STATUS_TIMEOUT_CANCELLED, paidAt, now, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order cancelIfTimedOut(LocalDateTime now, long timeoutMinutes) { return timedOut(now) ? cancelByTimeout(now) : this; }
    public Order markPaid(LocalDateTime now) { if (!pendingPayment() && !paymentException()) return this; return copy(STATUS_PAID, paidAt == null ? now : paidAt, cancelledAt, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark, expireTime); }
    public Order markPaidIfPossible(LocalDateTime now) { return pendingPayment() || paymentException() ? markPaid(now) : this; }
    public Order markPaymentException(String note) { if (!pendingPayment() && !paid() && !processing()) throw BusinessException.badRequest("仅待支付、已支付或处理中订单可标记支付异常"); return copy(STATUS_PAYMENT_EXCEPTION, paidAt, cancelledAt, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, appendRemarkText("PAYMENT_EXCEPTION_FROM_" + orderStatus, note), null); }
    public Order restorePendingPayment(String note) { if (!paymentException()) throw BusinessException.badRequest("仅支付异常订单可恢复待支付"); return copy(STATUS_PENDING_PAYMENT, paidAt, cancelledAt, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, appendRemarkText("RESTORE_PENDING_PAYMENT", note), LocalDateTime.now().plusMinutes(15)); }
    public Order markProcessing() { if (!paid()) throw BusinessException.badRequest("当前订单状态不允许进入处理中"); return copy(STATUS_PROCESSING, paidAt, cancelledAt, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order ship(LocalDateTime now) { if (paymentException()) throw BusinessException.badRequest("支付异常订单需先人工确认支付或恢复待支付"); if (!paid() && !processing()) throw BusinessException.badRequest("当前订单状态不允许发货"); return copy(STATUS_SHIPPED, paidAt, cancelledAt, now, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order complete(LocalDateTime now) { if (!paid() && !shipped()) throw BusinessException.badRequest("当前订单状态不允许确认收货"); return copy(STATUS_COMPLETED, paidAt, cancelledAt, shippedAt, now, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order markRefundPending() { ensureRefundable(); return copy(STATUS_REFUND_PENDING, paidAt, cancelledAt, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order rejectRefundToCompleted(String note) { return restoreFromRefundRejected(STATUS_COMPLETED, note); }
    public Order restoreFromRefundRejected(String targetStatus, String note) {
        if (!refundPending()) throw BusinessException.badRequest("仅退款中订单可驳回退款申请");
        String safeStatus = (targetStatus == null || targetStatus.isBlank()) ? STATUS_COMPLETED : targetStatus.trim();
        if (!STATUS_PAID.equals(safeStatus) && !STATUS_PROCESSING.equals(safeStatus) && !STATUS_SHIPPED.equals(safeStatus) && !STATUS_COMPLETED.equals(safeStatus)) {
            safeStatus = STATUS_COMPLETED;
        }
        LocalDateTime targetCompletedAt = STATUS_COMPLETED.equals(safeStatus) && completedAt == null ? LocalDateTime.now() : completedAt;
        return copy(safeStatus, paidAt, cancelledAt, shippedAt, targetCompletedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, appendRemarkText("REFUND_REJECTED", note));
    }
    public Order markRefundPendingByNegotiation(String note) { ensureRefundable(); return copy(STATUS_REFUND_PENDING, paidAt, cancelledAt, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, appendRemarkText("NEGOTIATION_RETURN", note)); }
    public void ensureNegotiatedReturnAllowed() { if (!shipped() && !completed()) throw BusinessException.badRequest("用户协商申请退货仅支持已发货或已完成订单"); }
    public Order keepStatusWithNegotiatedRefundApplication(String note) { ensureNegotiatedReturnAllowed(); return copy(orderStatus, paidAt, cancelledAt, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, appendRemarkText("NEGOTIATION_REFUND_APPLICATION", note)); }
    public Order returnToPaidForLogisticsException(String note) { if (!shipped()) throw BusinessException.badRequest("仅已发货订单可模拟物流异常回退"); return copy(STATUS_PAID, paidAt, cancelledAt, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, appendRemarkText("LOGISTICS_EXCEPTION_RETURN_TO_PAID", note)); }
    public void ensureSkuSwitchable() { if (!STATUS_PAID.equals(orderStatus) && !STATUS_PROCESSING.equals(orderStatus)) throw BusinessException.badRequest("仅已支付或处理中且发货前订单可协商切换SKU"); }
    public Order refundSuccess(LocalDateTime now) { if (!refundPending() && !refundable()) throw BusinessException.badRequest("当前订单状态不允许退款成功"); return copy(STATUS_REFUNDED, paidAt, now, shippedAt, completedAt == null ? now : completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order partialRefundSuccess(LocalDateTime now) { if (!refundable()) throw BusinessException.badRequest("当前订单状态不允许部分退款成功"); return copy(STATUS_PARTIALLY_REFUNDED, paidAt, cancelledAt, shippedAt, completedAt == null ? now : completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order reviseReceiver(String newReceiverName, String newReceiverPhone, String newReceiverProvinceName, String newReceiverCityName, String newReceiverDistrictName, String newReceiverDetailAddress) { if (!paid() && !processing()) throw BusinessException.badRequest("当前订单状态不允许修改收货信息"); requireText(newReceiverName, "收货人不能为空"); requireText(newReceiverPhone, "收货电话不能为空"); requireText(newReceiverProvinceName, "收货省份不能为空"); requireText(newReceiverCityName, "收货城市不能为空"); requireText(newReceiverDistrictName, "收货区县不能为空"); requireText(newReceiverDetailAddress, "收货详细地址不能为空"); return copy(orderStatus, paidAt, cancelledAt, shippedAt, completedAt, newReceiverName.trim(), newReceiverPhone.trim(), newReceiverProvinceName.trim(), newReceiverCityName.trim(), newReceiverDistrictName.trim(), newReceiverDetailAddress.trim(), remark); }
    public boolean hasSameReceiver(Order other) { return other != null && textEquals(receiverName, other.receiverName()) && textEquals(receiverPhone, other.receiverPhone()) && textEquals(receiverProvinceName, other.receiverProvinceName()) && textEquals(receiverCityName, other.receiverCityName()) && textEquals(receiverDistrictName, other.receiverDistrictName()) && textEquals(receiverDetailAddress, other.receiverDetailAddress()); }
    public Order appendAdminRemark(String note) { return copy(orderStatus, paidAt, cancelledAt, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, appendRemarkText(null, note)); }
    public boolean shouldReleaseStockAfterCancelled(Order previous) { return previous != null && (STATUS_CANCELLED.equals(orderStatus) || STATUS_TIMEOUT_CANCELLED.equals(orderStatus) || STATUS_REFUNDED.equals(orderStatus)) && !previous.cancelled(); }
    public boolean shouldConfirmStockAfterPaid(Order previous) { return STATUS_PAID.equals(orderStatus) && previous != null && (previous.pendingPayment() || previous.paymentException()); }
    public boolean shouldIncrementSalesAfterCompleted(Order previous) { return STATUS_COMPLETED.equals(orderStatus) && previous != null && !previous.completed(); }
    public boolean shouldRollbackSalesAfterRefundSuccess(Order previous) { return STATUS_REFUNDED.equals(orderStatus) && previous != null && previous.completed(); }
    public int itemCount() { return items == null ? 0 : items.stream().mapToInt(item -> item.quantity() == null ? 0 : item.quantity()).sum(); }
    public long safePayAmountCent() { return payAmountCent == null ? 0L : payAmountCent; }

    public Order copy(String status, LocalDateTime paidAt, LocalDateTime cancelledAt, LocalDateTime shippedAt, LocalDateTime completedAt, String receiverName, String receiverPhone, String receiverProvinceName, String receiverCityName, String receiverDistrictName, String receiverDetailAddress, String remark) {
        return copy(status, paidAt, cancelledAt, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark, expireTime);
    }

    public Order copy(String status, LocalDateTime paidAt, LocalDateTime cancelledAt, LocalDateTime shippedAt, LocalDateTime completedAt, String receiverName, String receiverPhone, String receiverProvinceName, String receiverCityName, String receiverDistrictName, String receiverDetailAddress, String remark, LocalDateTime expireTime) {
        return new Order(id, orderNo, userId, status, totalAmountCent, payAmountCent, freightAmountCent, discountAmountCent, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark, payType, paidAt, cancelledAt, shippedAt, completedAt, expireTime, createdAt, nextVersion(), items);
    }

    private void ensurePendingPayment(String message) { if (!pendingPayment()) throw BusinessException.badRequest(message); }
    private void ensureAdminClosable() { if (!pendingPayment() && !paymentException()) throw BusinessException.badRequest("当前订单状态不允许后台关闭"); }
    private void requireText(String value, String message) { if (value == null || value.isBlank()) throw BusinessException.badRequest(message); }
    private String appendRemarkText(String marker, String note) { String normalizedNote = note == null || note.isBlank() ? marker : (marker == null || marker.isBlank() ? note.trim() : marker + ":" + note.trim()); return (remark == null || remark.isBlank()) ? normalizedNote : remark + " | " + normalizedNote; }
    private boolean textEquals(String left, String right) { return java.util.Objects.equals(left == null ? "" : left.trim(), right == null ? "" : right.trim()); }
    private int nextVersion() { return version == null ? 0 : version + 1; }
}
