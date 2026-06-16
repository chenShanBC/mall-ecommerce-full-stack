-- 商品完成销量日统计：用于后台真实近30天销量、热销/低销、商品仪表盘
CREATE TABLE IF NOT EXISTS `pms_product_sales_daily_stat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stat_date` date NOT NULL COMMENT '统计日期，按订单完成时间归档',
  `spu_id` bigint NOT NULL COMMENT '商品SPU ID',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `sale_channel` varchar(32) NOT NULL DEFAULT 'NORMAL' COMMENT '销售渠道：NORMAL普通，SECKILL秒杀预留',
  `completed_quantity` int NOT NULL DEFAULT 0 COMMENT '完成订单销量',
  `completed_amount_cent` bigint NOT NULL DEFAULT 0 COMMENT '完成订单销售额，单位分',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_sku_channel` (`stat_date`, `sku_id`, `sale_channel`),
  KEY `idx_spu_date` (`spu_id`, `stat_date`),
  KEY `idx_sku_date` (`sku_id`, `stat_date`),
  KEY `idx_channel_date` (`sale_channel`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品每日完成销量统计表';

CREATE TABLE IF NOT EXISTS `pms_product_sales_stat_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_key` varchar(128) NOT NULL COMMENT '事件唯一键，如 ORDER_COMPLETED:订单号',
  `event_type` varchar(32) NOT NULL COMMENT '事件类型',
  `biz_no` varchar(64) NOT NULL COMMENT '业务单号',
  `handled_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_key` (`event_key`),
  KEY `idx_biz_no` (`biz_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品销售统计事件幂等表';

-- 历史完成订单补数：按订单 completed_at 回灌普通渠道完成销量
INSERT INTO `pms_product_sales_daily_stat` (
  `stat_date`, `spu_id`, `sku_id`, `sale_channel`, `completed_quantity`, `completed_amount_cent`
)
SELECT
  DATE(o.`completed_at`) AS `stat_date`,
  oi.`spu_id`,
  oi.`sku_id`,
  'NORMAL' AS `sale_channel`,
  SUM(oi.`quantity`) AS `completed_quantity`,
  SUM(oi.`total_amount_cent`) AS `completed_amount_cent`
FROM `oms_order` o
JOIN `oms_order_item` oi ON oi.`order_no` = o.`order_no` AND oi.`deleted_at` IS NULL
WHERE o.`order_status` = 'COMPLETED'
  AND o.`completed_at` IS NOT NULL
  AND o.`deleted_at` IS NULL
GROUP BY DATE(o.`completed_at`), oi.`spu_id`, oi.`sku_id`
ON DUPLICATE KEY UPDATE
  `completed_quantity` = VALUES(`completed_quantity`),
  `completed_amount_cent` = VALUES(`completed_amount_cent`),
  `updated_at` = NOW();

INSERT IGNORE INTO `pms_product_sales_stat_event` (`event_key`, `event_type`, `biz_no`)
SELECT CONCAT('ORDER_COMPLETED:', o.`order_no`), 'ORDER_COMPLETED', o.`order_no`
FROM `oms_order` o
WHERE o.`order_status` = 'COMPLETED'
  AND o.`completed_at` IS NOT NULL
  AND o.`deleted_at` IS NULL;
