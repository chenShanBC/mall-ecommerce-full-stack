CREATE TABLE IF NOT EXISTS `oms_order_refund_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `refund_id` BIGINT NOT NULL COMMENT '退款记录ID',
  `refund_no` VARCHAR(64) NOT NULL COMMENT '退款单号',
  `order_item_id` BIGINT NOT NULL COMMENT '订单明细ID',
  `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
  `quantity` INT NOT NULL COMMENT '退款数量',
  `refund_amount_cent` BIGINT NOT NULL COMMENT '退款金额，单位分',
  PRIMARY KEY (`id`),
  KEY `idx_oms_order_refund_item_refund_no` (`refund_no`),
  KEY `idx_oms_order_refund_item_order_item_id` (`order_item_id`),
  KEY `idx_oms_order_refund_item_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单退款明细表';
