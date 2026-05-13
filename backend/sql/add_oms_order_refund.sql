CREATE TABLE IF NOT EXISTS oms_order_refund (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '退款申请ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    refund_status VARCHAR(32) NOT NULL COMMENT '退款状态',
    refund_reason VARCHAR(200) NOT NULL COMMENT '退款原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_oms_order_refund_order_no (order_no),
    KEY idx_oms_order_refund_user_id (user_id)
) COMMENT='订单退款申请表';
