CREATE TABLE IF NOT EXISTS pay_callback_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '回调记录ID',
    channel VARCHAR(64) NOT NULL COMMENT '支付渠道',
    pay_order_no VARCHAR(64) NULL COMMENT '支付单号',
    order_no VARCHAR(64) NULL COMMENT '订单号',
    out_trade_no VARCHAR(64) NULL COMMENT '商户订单号',
    transaction_no VARCHAR(128) NULL COMMENT '第三方交易号',
    signature VARCHAR(512) NULL COMMENT '签名信息',
    verified TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否验签通过',
    process_status VARCHAR(64) NOT NULL DEFAULT 'INIT' COMMENT '处理状态',
    raw_payload TEXT NULL COMMENT '原始回调报文',
    callback_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回调时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_pay_callback_record_channel (channel),
    KEY idx_pay_callback_record_pay_order_no (pay_order_no),
    KEY idx_pay_callback_record_order_no (order_no),
    KEY idx_pay_callback_record_out_trade_no (out_trade_no)
) COMMENT='支付回调记录表';
