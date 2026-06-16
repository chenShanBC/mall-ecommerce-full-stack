CREATE TABLE IF NOT EXISTS `pay_refund_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付退款单ID',
  `refund_no` VARCHAR(64) NOT NULL COMMENT '本地退款单号',
  `order_no` VARCHAR(64) NOT NULL COMMENT '业务订单号',
  `pay_order_no` VARCHAR(64) NOT NULL COMMENT '原支付单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `pay_channel` VARCHAR(30) NOT NULL DEFAULT 'MOCK' COMMENT '支付渠道',
  `refund_amount_cent` BIGINT NOT NULL COMMENT '退款金额分',
  `refund_status` VARCHAR(32) NOT NULL COMMENT '退款状态：REFUND_PENDING/REFUNDING/REFUND_SUCCESS/REFUND_FAILED',
  `transaction_no` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '原三方交易号',
  `channel_refund_no` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '渠道退款流水号',
  `request_payload` TEXT NULL COMMENT '退款请求摘要',
  `response_payload` TEXT NULL COMMENT '退款响应摘要',
  `fail_reason` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '失败原因',
  `success_at` DATETIME NULL DEFAULT NULL COMMENT '退款成功时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_refund_order_refund_no` (`refund_no`),
  KEY `idx_pay_refund_order_order_no` (`order_no`),
  KEY `idx_pay_refund_order_pay_order_no` (`pay_order_no`),
  KEY `idx_pay_refund_order_status_id` (`refund_status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付退款单表';

CREATE TABLE IF NOT EXISTS `pay_reconciliation_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付对账记录ID',
  `batch_no` VARCHAR(64) DEFAULT NULL COMMENT '对账批次号',
  `biz_type` VARCHAR(16) NOT NULL COMMENT '业务类型：PAY/REFUND',
  `order_no` VARCHAR(64) NOT NULL COMMENT '业务订单号',
  `pay_order_no` VARCHAR(64) DEFAULT NULL COMMENT '支付单号',
  `refund_no` VARCHAR(64) DEFAULT NULL COMMENT '退款单号',
  `local_status` VARCHAR(64) DEFAULT NULL COMMENT '本地状态',
  `channel_status` VARCHAR(64) DEFAULT NULL COMMENT '渠道状态',
  `local_amount_cent` BIGINT DEFAULT NULL COMMENT '本地金额分',
  `channel_amount_cent` BIGINT DEFAULT NULL COMMENT '渠道金额分',
  `consistent` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否一致',
  `diff_type` VARCHAR(64) NOT NULL DEFAULT 'NONE' COMMENT '差异类型',
  `repair_status` VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '修复状态：NONE/PENDING/DONE/IGNORED',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `repaired_at` DATETIME NULL DEFAULT NULL COMMENT '修复时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_pay_reconciliation_batch_no` (`batch_no`),
  KEY `idx_pay_reconciliation_biz_order` (`biz_type`, `order_no`),
  KEY `idx_pay_reconciliation_status_time` (`consistent`, `created_at`),
  KEY `idx_pay_reconciliation_refund_no` (`refund_no`),
  KEY `idx_pay_reconciliation_pay_order_no` (`pay_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付退款轻量对账记录表';

DROP PROCEDURE IF EXISTS `mallfei_add_column_if_missing`;
DELIMITER $$
CREATE PROCEDURE `mallfei_add_column_if_missing`(
  IN p_table_name VARCHAR(64),
  IN p_column_name VARCHAR(64),
  IN p_column_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN ', p_column_definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS `mallfei_add_index_if_missing`;
DELIMITER $$
CREATE PROCEDURE `mallfei_add_index_if_missing`(
  IN p_table_name VARCHAR(64),
  IN p_index_name VARCHAR(64),
  IN p_index_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND INDEX_NAME = p_index_name
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD ', p_index_definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL `mallfei_add_column_if_missing`('oms_order_refund', 'refund_no', '`refund_no` VARCHAR(64) DEFAULT NULL COMMENT ''退款单号'' AFTER `user_id`');
CALL `mallfei_add_column_if_missing`('oms_order_refund', 'refund_amount_cent', '`refund_amount_cent` BIGINT DEFAULT NULL COMMENT ''退款金额，单位分'' AFTER `refund_no`');
CALL `mallfei_add_column_if_missing`('oms_order_refund', 'channel_refund_no', '`channel_refund_no` VARCHAR(128) DEFAULT NULL COMMENT ''渠道退款流水号'' AFTER `refund_amount_cent`');
CALL `mallfei_add_column_if_missing`('oms_order_refund', 'fail_reason', '`fail_reason` VARCHAR(512) DEFAULT NULL COMMENT ''失败原因'' AFTER `refund_reason`');

UPDATE `oms_order_refund`
SET `refund_no` = CONCAT('ORF', `id`)
WHERE (`refund_no` IS NULL OR `refund_no` = '') AND `id` IS NOT NULL;

ALTER TABLE `oms_order_refund`
  MODIFY COLUMN `refund_no` VARCHAR(64) NOT NULL COMMENT '退款单号';

CALL `mallfei_add_index_if_missing`('oms_order_refund', 'uk_oms_order_refund_refund_no', 'UNIQUE KEY `uk_oms_order_refund_refund_no` (`refund_no`)');
CALL `mallfei_add_index_if_missing`('oms_order_refund', 'idx_oms_order_refund_status', 'KEY `idx_oms_order_refund_status` (`refund_status`)');

CALL `mallfei_add_column_if_missing`('pay_reconciliation_record', 'batch_no', '`batch_no` VARCHAR(64) DEFAULT NULL COMMENT ''对账批次号'' AFTER `id`');
CALL `mallfei_add_index_if_missing`('pay_reconciliation_record', 'idx_pay_reconciliation_batch_no', 'KEY `idx_pay_reconciliation_batch_no` (`batch_no`)');

CALL `mallfei_add_column_if_missing`('pay_callback_record', 'callback_type', '`callback_type` VARCHAR(16) NOT NULL DEFAULT ''PAY'' COMMENT ''回调类型：PAY/REFUND'' AFTER `channel`');
CALL `mallfei_add_column_if_missing`('pay_callback_record', 'refund_no', '`refund_no` VARCHAR(64) DEFAULT NULL COMMENT ''退款单号'' AFTER `pay_order_no`');
CALL `mallfei_add_column_if_missing`('pay_callback_record', 'amount_cent', '`amount_cent` BIGINT DEFAULT NULL COMMENT ''金额分'' AFTER `transaction_no`');
CALL `mallfei_add_column_if_missing`('pay_callback_record', 'trade_status', '`trade_status` VARCHAR(32) DEFAULT NULL COMMENT ''渠道交易状态'' AFTER `amount_cent`');
CALL `mallfei_add_column_if_missing`('pay_callback_record', 'fail_reason', '`fail_reason` VARCHAR(512) DEFAULT NULL COMMENT ''失败原因'' AFTER `process_status`');
CALL `mallfei_add_column_if_missing`('pay_callback_record', 'processed_at', '`processed_at` DATETIME NULL DEFAULT NULL COMMENT ''处理时间'' AFTER `callback_time`');

CALL `mallfei_add_index_if_missing`('pay_callback_record', 'idx_pay_callback_record_refund_no', 'KEY `idx_pay_callback_record_refund_no` (`refund_no`)');
CALL `mallfei_add_index_if_missing`('pay_callback_record', 'idx_pay_callback_record_created_at', 'KEY `idx_pay_callback_record_created_at` (`created_at`)');

DROP PROCEDURE IF EXISTS `mallfei_add_column_if_missing`;
DROP PROCEDURE IF EXISTS `mallfei_add_index_if_missing`;
