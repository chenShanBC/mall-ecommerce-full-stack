package com.mallfei.common.enums;

/**
 * 订单状态枚举。
 */
public enum OrderStatus {

    /** 待支付：订单已创建，等待用户完成支付。 */
    PENDING_PAYMENT("PENDING_PAYMENT", "待支付"),
    /** 已支付：支付成功，等待商家处理或发货。 */
    PAID("PAID", "已支付"),
    /** 处理中：已支付后进入配货、拣货、打包等处理中阶段。 */
    PROCESSING("PROCESSING", "处理中"),
    /** 已发货：商家已完成发货，等待用户签收。 */
    SHIPPED("SHIPPED", "已发货"),
    /** 已完成：用户确认收货或系统自动完成，订单交易闭环完成。 */
    COMPLETED("COMPLETED", "已完成"),
    /** 已取消：订单被用户、系统或平台主动取消。 */
    CANCELLED("CANCELLED", "已取消"),
    /** 超时取消：订单在规定时间内未支付，被系统自动取消。 */
    TIMEOUT_CANCELLED("TIMEOUT_CANCELLED", "超时取消"),
    /** 已关闭：订单被平台关闭，不再允许继续处理。 */
    CLOSED("CLOSED", "已关闭"),
    /** 退款中：用户已发起退款申请，平台或系统处理中。 */
    REFUND_PENDING("REFUND_PENDING", "退款中"),
    /** 已退款：退款已原路退回，订单进入退款完成状态。 */
    REFUNDED("REFUNDED", "已退款"),
    /** 退款关闭：退款申请被关闭、撤销或驳回。 */
    REFUND_CLOSED("REFUND_CLOSED", "退款关闭"),
    /** 部分退款：订单发生部分商品或部分金额退款。 */
    PARTIALLY_REFUNDED("PARTIALLY_REFUNDED", "部分退款");

    /** 状态编码。 */
    private final String code;
    /** 状态中文描述。 */
    private final String description;

    OrderStatus(String code, String description) {
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
