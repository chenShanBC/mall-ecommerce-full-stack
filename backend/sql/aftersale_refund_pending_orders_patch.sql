-- 补全三条 REFUND_PENDING 订单对应的售后单、订单退款单、退款明细与支付退款单。
-- 适用订单：
--   ORD1780412266696D7BE2F / ¥899.00
--   ORD1780411012113EC7D4C / ¥158.00
--   ORD1780374433688BC4A87 / ¥188.00

START TRANSACTION;

-- 1) 售后单：订单已经进入 REFUND_PENDING，这里补为退款处理中，并绑定退款单号。
INSERT INTO `aftersale_order` (
  `aftersale_no`, `order_no`, `user_id`, `aftersale_type`, `status`, `origin_order_status`,
  `refund_amount_cent`, `reason`, `reject_reason`, `refund_no`, `fail_reason`, `version`,
  `reviewed_at`, `created_at`, `updated_at`
)
SELECT
  CONCAT('AS', o.`order_no`) AS `aftersale_no`,
  o.`order_no`,
  o.`user_id`,
  'ONLY_REFUND' AS `aftersale_type`,
  'REFUND_PROCESSING' AS `status`,
  'PAID' AS `origin_order_status`,
  o.`pay_amount_cent` AS `refund_amount_cent`,
  '用户发起退款，补全历史售后单数据' AS `reason`,
  NULL AS `reject_reason`,
  CONCAT('ORF', o.`order_no`) AS `refund_no`,
  NULL AS `fail_reason`,
  1 AS `version`,
  o.`updated_at` AS `reviewed_at`,
  o.`updated_at` AS `created_at`,
  o.`updated_at` AS `updated_at`
FROM `oms_order` o
WHERE o.`order_no` IN ('ORD1780412266696D7BE2F', 'ORD1780411012113EC7D4C', 'ORD1780374433688BC4A87')
  AND NOT EXISTS (
    SELECT 1 FROM `aftersale_order` ao WHERE ao.`order_no` = o.`order_no`
  );

-- 2) 订单退款主单。
INSERT INTO `oms_order_refund` (
  `order_id`, `order_no`, `user_id`, `refund_no`, `refund_amount_cent`, `channel_refund_no`,
  `refund_status`, `refund_reason`, `fail_reason`, `created_at`, `updated_at`
)
SELECT
  o.`id`,
  o.`order_no`,
  o.`user_id`,
  CONCAT('ORF', o.`order_no`) AS `refund_no`,
  o.`pay_amount_cent` AS `refund_amount_cent`,
  NULL AS `channel_refund_no`,
  'REFUND_PENDING' AS `refund_status`,
  '用户发起退款，补全历史退款单数据' AS `refund_reason`,
  NULL AS `fail_reason`,
  o.`updated_at` AS `created_at`,
  o.`updated_at` AS `updated_at`
FROM `oms_order` o
WHERE o.`order_no` IN ('ORD1780412266696D7BE2F', 'ORD1780411012113EC7D4C', 'ORD1780374433688BC4A87')
  AND NOT EXISTS (
    SELECT 1 FROM `oms_order_refund` r WHERE r.`order_no` = o.`order_no`
  );

-- 3) 订单退款明细：按订单明细补全，三条订单当前均为单 SKU。
INSERT INTO `oms_order_refund_item` (
  `refund_id`, `refund_no`, `order_item_id`, `sku_id`, `quantity`, `refund_amount_cent`
)
SELECT
  r.`id` AS `refund_id`,
  r.`refund_no`,
  oi.`id` AS `order_item_id`,
  oi.`sku_id`,
  oi.`quantity`,
  oi.`total_amount_cent` AS `refund_amount_cent`
FROM `oms_order_refund` r
JOIN `oms_order_item` oi ON oi.`order_no` = r.`order_no`
WHERE r.`order_no` IN ('ORD1780412266696D7BE2F', 'ORD1780411012113EC7D4C', 'ORD1780374433688BC4A87')
  AND NOT EXISTS (
    SELECT 1 FROM `oms_order_refund_item` ri
    WHERE ri.`refund_no` = r.`refund_no` AND ri.`order_item_id` = oi.`id`
  );

-- 4) 支付退款单：退款已提交但未完成，支付侧补为 REFUND_PENDING，方便后续退款任务/回调继续推进。
INSERT INTO `pay_refund_order` (
  `refund_no`, `order_no`, `pay_order_no`, `user_id`, `pay_channel`, `refund_amount_cent`,
  `refund_status`, `transaction_no`, `channel_refund_no`, `request_payload`, `response_payload`,
  `fail_reason`, `success_at`, `version`, `created_at`, `updated_at`
)
SELECT
  r.`refund_no`,
  o.`order_no`,
  p.`pay_order_no`,
  o.`user_id`,
  p.`pay_channel`,
  r.`refund_amount_cent`,
  'REFUND_PENDING' AS `refund_status`,
  COALESCE(p.`transaction_no`, '') AS `transaction_no`,
  '' AS `channel_refund_no`,
  JSON_OBJECT('source', 'manual_patch', 'orderNo', o.`order_no`, 'refundNo', r.`refund_no`, 'amountCent', r.`refund_amount_cent`) AS `request_payload`,
  NULL AS `response_payload`,
  '' AS `fail_reason`,
  NULL AS `success_at`,
  0 AS `version`,
  r.`created_at`,
  r.`updated_at`
FROM `oms_order_refund` r
JOIN `oms_order` o ON o.`order_no` = r.`order_no`
JOIN `pay_order` p ON p.`order_no` = o.`order_no` AND p.`pay_status` = 'SUCCESS'
WHERE r.`order_no` IN ('ORD1780412266696D7BE2F', 'ORD1780411012113EC7D4C', 'ORD1780374433688BC4A87')
  AND NOT EXISTS (
    SELECT 1 FROM `pay_refund_order` pro WHERE pro.`refund_no` = r.`refund_no`
  );

COMMIT;
