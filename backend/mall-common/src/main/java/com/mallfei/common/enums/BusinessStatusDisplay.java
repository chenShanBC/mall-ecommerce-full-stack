package com.mallfei.common.enums;

import java.util.Map;

/**
 * Cross-module status display dictionary.
 *
 * <p>Domain aggregates still keep their own real status codes. This class only
 * centralizes user/admin display semantics so order, pay, refund and aftersale
 * modules do not duplicate label decisions everywhere.</p>
 */
public final class BusinessStatusDisplay {

    private static final StatusMeta UNKNOWN = new StatusMeta("-", "info");

    private static final Map<String, StatusMeta> ORDER = Map.ofEntries(
            entry("PENDING_PAYMENT", "待支付", "warning"),
            entry("PAID", "已支付", "primary"),
            entry("PAYMENT_EXCEPTION", "支付异常", "danger"),
            entry("PROCESSING", "处理中", "primary"),
            entry("SHIPPED", "已发货", "primary"),
            entry("COMPLETED", "已完成", "success"),
            entry("CANCELLED", "已取消", "info"),
            entry("TIMEOUT_CANCELLED", "超时取消", "danger"),
            entry("CLOSED", "已关闭", "info"),
            entry("REFUND_PENDING", "售后中", "warning"),
            entry("REFUNDED", "已退款", "danger"),
            entry("PARTIALLY_REFUNDED", "部分退款", "warning"),
            entry("REFUND_CLOSED", "退款关闭", "info")
    );

    private static final Map<String, StatusMeta> PAY = Map.ofEntries(
            entry("PENDING", "待支付", "warning"),
            entry("PAYING", "支付中", "primary"),
            entry("SUCCESS", "支付成功", "success"),
            entry("FAILED", "支付失败", "danger"),
            entry("CLOSED", "已关闭", "info"),
            entry("REFUND_PENDING", "退款中", "warning"),
            entry("REFUNDING", "退款中", "primary"),
            entry("REFUNDED", "已退款", "danger"),
            entry("PARTIALLY_REFUNDED", "部分退款", "warning"),
            entry("REFUND_FAILED", "退款失败", "danger")
    );

    private static final Map<String, StatusMeta> REFUND = Map.ofEntries(
            entry("REFUND_PENDING", "待退款", "warning"),
            entry("REFUNDING", "退款中", "primary"),
            entry("REFUND_SUCCESS", "退款成功", "success"),
            entry("REFUND_FAILED", "退款失败", "danger"),
            entry("REFUND_CLOSED", "退款关闭", "info")
    );

    private static final Map<String, StatusMeta> AFTERSALE = Map.ofEntries(
            entry("PENDING_REVIEW", "售后中", "warning"),
            entry("APPROVED", "审核通过", "success"),
            entry("REJECTED", "审核驳回", "danger"),
            entry("REFUND_PROCESSING", "退款中", "primary"),
            entry("REFUND_SUCCESS", "退款成功", "success"),
            entry("REFUND_FAILED", "退款失败", "danger"),
            entry("CANCELLED", "已取消", "info")
    );

    private BusinessStatusDisplay() {
    }

    public static StatusMeta order(String code) {
        return resolve(ORDER, code);
    }

    public static StatusMeta pay(String code) {
        return resolve(PAY, code);
    }

    public static StatusMeta refund(String code) {
        return resolve(REFUND, code);
    }

    public static StatusMeta aftersale(String code) {
        return resolve(AFTERSALE, code);
    }

    private static StatusMeta resolve(Map<String, StatusMeta> source, String code) {
        if (code == null || code.isBlank()) {
            return UNKNOWN;
        }
        return source.getOrDefault(code.trim().toUpperCase(), new StatusMeta(code, "info"));
    }

    private static Map.Entry<String, StatusMeta> entry(String code, String label, String type) {
        return Map.entry(code, new StatusMeta(label, type));
    }

    public record StatusMeta(String label, String type) {
    }
}
