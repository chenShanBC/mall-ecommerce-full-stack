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

    public static Order createPending(String orderNo, Long userId, Long totalAmountCent, String receiverName, String receiverPhone, String receiverProvinceName, String receiverCityName, String receiverDistrictName, String receiverDetailAddress, String remark, List<OrderItem> items) {
        return new Order(null, orderNo, userId, STATUS_PENDING_PAYMENT, totalAmountCent, totalAmountCent, 0L, 0L, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark == null ? "" : remark, "MOCK", null, null, null, null, 0, items);
    }

    public boolean belongsTo(Long targetUserId) { return userId != null && userId.equals(targetUserId); }
    public boolean pendingPayment() { return STATUS_PENDING_PAYMENT.equals(orderStatus); }
    public boolean paid() { return STATUS_PAID.equals(orderStatus) || STATUS_PROCESSING.equals(orderStatus); }
    public boolean processing() { return STATUS_PROCESSING.equals(orderStatus); }
    public boolean shipped() { return STATUS_SHIPPED.equals(orderStatus); }
    public boolean completed() { return STATUS_COMPLETED.equals(orderStatus); }
    public boolean refunded() { return STATUS_REFUNDED.equals(orderStatus) || STATUS_PARTIALLY_REFUNDED.equals(orderStatus); }
    public boolean paidOrAfter() { return STATUS_PAID.equals(orderStatus) || STATUS_PROCESSING.equals(orderStatus) || STATUS_SHIPPED.equals(orderStatus) || STATUS_COMPLETED.equals(orderStatus) || STATUS_REFUND_PENDING.equals(orderStatus) || STATUS_REFUNDED.equals(orderStatus) || STATUS_PARTIALLY_REFUNDED.equals(orderStatus); }
    public boolean cancelled() { return STATUS_CANCELLED.equals(orderStatus) || STATUS_TIMEOUT_CANCELLED.equals(orderStatus) || STATUS_CLOSED.equals(orderStatus) || STATUS_REFUNDED.equals(orderStatus); }
    public boolean timeoutCancelled() { return STATUS_TIMEOUT_CANCELLED.equals(orderStatus); }
    public boolean refundPending() { return STATUS_REFUND_PENDING.equals(orderStatus); }
    public boolean refundable() { return paid() || shipped(); }

    public void ensureRefundable() {
        if (!refundable()) throw BusinessException.badRequest(completed() ? "订单已确认收货，不支持退款" : "当前订单状态不允许申请退款");
    }

    public boolean timedOut(LocalDateTime now, long timeoutMinutes) { if (!pendingPayment()) return false; return now.isAfter(createdAtFromOrderNo().plusMinutes(timeoutMinutes)); }
    public LocalDateTime createdAtFromOrderNo() { try { long timestamp = Long.parseLong(orderNo.substring(3, 16)); return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()); } catch (Exception ignored) { return LocalDateTime.now(); } }
    public Order cancelByUser(LocalDateTime now) { ensurePendingPayment("当前订单状态不允许取消"); return copy(STATUS_CANCELLED, paidAt, now, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order cancelByAdmin(LocalDateTime now) { ensurePendingPayment("当前订单状态不允许后台取消"); return copy(STATUS_CANCELLED, paidAt, now, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order cancelByTimeout(LocalDateTime now) { if (!pendingPayment()) return this; return copy(STATUS_TIMEOUT_CANCELLED, paidAt, now, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order cancelIfTimedOut(LocalDateTime now, long timeoutMinutes) { return timedOut(now, timeoutMinutes) ? cancelByTimeout(now) : this; }
    public Order markPaid(LocalDateTime now) { if (!pendingPayment()) return this; return copy(STATUS_PAID, now, cancelledAt, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order markPaidIfPossible(LocalDateTime now) { return pendingPayment() ? markPaid(now) : this; }
    public Order markProcessing() { if (!paid()) throw BusinessException.badRequest("当前订单状态不允许进入处理中"); return copy(STATUS_PROCESSING, paidAt, cancelledAt, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order ship(LocalDateTime now) { if (!paid() && !processing()) throw BusinessException.badRequest("当前订单状态不允许发货"); return copy(STATUS_SHIPPED, paidAt, cancelledAt, now, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order complete(LocalDateTime now) { if (!paid() && !shipped()) throw BusinessException.badRequest("当前订单状态不允许确认收货"); return copy(STATUS_COMPLETED, paidAt, cancelledAt, shippedAt, now, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order markRefundPending() { ensureRefundable(); return copy(STATUS_REFUND_PENDING, paidAt, cancelledAt, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order refundSuccess(LocalDateTime now) { if (!refundPending() && !refundable()) throw BusinessException.badRequest("当前订单状态不允许退款成功"); return copy(STATUS_REFUNDED, paidAt, now, shippedAt, completedAt == null ? now : completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark); }
    public Order reviseReceiver(String newReceiverName, String newReceiverPhone, String newReceiverDetailAddress) { if (shipped() || completed() || cancelled()) throw BusinessException.badRequest("当前订单状态不允许修改收货信息"); return copy(orderStatus, paidAt, cancelledAt, shippedAt, completedAt, newReceiverName, newReceiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, newReceiverDetailAddress, remark); }
    public Order appendAdminRemark(String note) { String nextRemark = (remark == null || remark.isBlank()) ? note : remark + " | " + note; return copy(orderStatus, paidAt, cancelledAt, shippedAt, completedAt, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, nextRemark); }
    public boolean shouldReleaseStockAfterCancelled(Order previous) { return (STATUS_CANCELLED.equals(orderStatus) || STATUS_TIMEOUT_CANCELLED.equals(orderStatus) || STATUS_REFUNDED.equals(orderStatus)) && !previous.cancelled(); }
    public int itemCount() { return items == null ? 0 : items.stream().mapToInt(item -> item.quantity() == null ? 0 : item.quantity()).sum(); }
    public long safePayAmountCent() { return payAmountCent == null ? 0L : payAmountCent; }

    public Order copy(String status, LocalDateTime paidAt, LocalDateTime cancelledAt, LocalDateTime shippedAt, LocalDateTime completedAt, String receiverName, String receiverPhone, String receiverProvinceName, String receiverCityName, String receiverDistrictName, String receiverDetailAddress, String remark) {
        return new Order(id, orderNo, userId, status, totalAmountCent, payAmountCent, freightAmountCent, discountAmountCent, receiverName, receiverPhone, receiverProvinceName, receiverCityName, receiverDistrictName, receiverDetailAddress, remark, payType, paidAt, cancelledAt, shippedAt, completedAt, nextVersion(), items);
    }

    private void ensurePendingPayment(String message) { if (!pendingPayment()) throw BusinessException.badRequest(message); }
    private int nextVersion() { return version == null ? 0 : version + 1; }
}
