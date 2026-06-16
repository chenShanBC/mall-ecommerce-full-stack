package com.mallfei.common.enums;

/**
 * 支付状态枚举。
 */
public enum PayStatus {

    /** 待支付：支付单已创建，尚未开始支付。 */
    PENDING("PENDING", "待支付"),
    /** 支付中：用户已拉起支付，第三方渠道处理中。 */
    PAYING("PAYING", "支付中"),
    /** 支付成功：渠道已确认支付成功。 */
    SUCCESS("SUCCESS", "支付成功"),
    /** 支付失败：渠道明确返回支付失败。 */
    FAILED("FAILED", "支付失败"),
    /** 已关闭：支付单关闭，不再允许支付。 */
    CLOSED("CLOSED", "已关闭"),
    /** 退款待处理：退款申请已创建，等待进入渠道退款。 */
    REFUND_PENDING("REFUND_PENDING", "退款待处理"),
    /** 退款中：已提交渠道，等待渠道退款结果。 */
    REFUNDING("REFUNDING", "退款中"),
    /** 已退款：已完成原路退款。 */
    REFUNDED("REFUNDED", "已退款"),
    /** 部分退款：支付单发生部分金额退款。 */
    PARTIALLY_REFUNDED("PARTIALLY_REFUNDED", "部分退款"),
    /** 退款失败：退款请求已执行但渠道返回失败。 */
    REFUND_FAILED("REFUND_FAILED", "退款失败");

    /** 状态编码。 */
    private final String code;
    /** 状态中文描述。 */
    private final String description;

    PayStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public String description() {
        return description;
    }
}
