ALTER TABLE `oms_order_refund`
  ADD COLUMN IF NOT EXISTS `refund_no` VARCHAR(64) DEFAULT NULL COMMENT '退款单号' AFTER `user_id`,
  ADD COLUMN IF NOT EXISTS `refund_amount_cent` BIGINT DEFAULT NULL COMMENT '退款金额，单位分' AFTER `refund_no`,
  ADD COLUMN IF NOT EXISTS `channel_refund_no` VARCHAR(64) DEFAULT NULL COMMENT '渠道退款流水号' AFTER `refund_amount_cent`,
  ADD COLUMN IF NOT EXISTS `fail_reason` VARCHAR(512) DEFAULT NULL COMMENT '失败原因' AFTER `refund_reason`;

UPDATE `oms_order_refund`
SET `refund_no` = CONCAT('ORF', `id`)
WHERE (`refund_no` IS NULL OR `refund_no` = '') AND `id` IS NOT NULL;

ALTER TABLE `oms_order_refund`
  MODIFY COLUMN `refund_no` VARCHAR(64) NOT NULL COMMENT '退款单号',
  ADD UNIQUE KEY IF NOT EXISTS `uk_oms_order_refund_refund_no` (`refund_no`),
  ADD KEY IF NOT EXISTS `idx_oms_order_refund_order_no` (`order_no`),
  ADD KEY IF NOT EXISTS `idx_oms_order_refund_status` (`refund_status`);
