/*
 * 支付异常“关闭并释放库存”专项测试数据补充脚本
 *
 * 用途：验证支付异常处理弹窗中“关闭并释放库存”按钮在不同核验结论下是否正确解锁/禁用。
 *
 * 说明：
 * 1. 本脚本可在已有数据基础上执行，订单号统一以 ORDPEXCLOSE20260602 开头。
 * 2. 重复执行会先清理同前缀测试数据。
 * 3. 订单均为 PAYMENT_EXCEPTION，方便直接进入“支付异常处理 -> 核验支付状态”。
 * 4. 每个订单都有订单项和库存锁记录，允许关闭的订单可形成“关闭订单 + 释放预占库存”的 DB 侧闭环。
 * 5. 如果你的库存服务依赖 Redis 库存状态，执行关闭动作时还需保证 Redis 侧已有对应库存锁；本脚本主要补齐 DB 侧闭环数据。
 *
 * 测试订单与预期：
 *
 * 允许关闭并释放库存：
 * - ORDPEXCLOSE202606020001：NO_PAY_ORDER / LOW / 允许 RESTORE_PENDING_PAYMENT、CLOSE_AND_RELEASE_STOCK
 * - ORDPEXCLOSE202606020002：UNPAID_VERIFIED / LOW / 本地 CLOSED，渠道 CLOSED / 允许关闭释放
 * - ORDPEXCLOSE202606020003：UNPAID_VERIFIED / LOW / 本地 FAILED，渠道 FAILED / 允许关闭释放
 *
 * 禁止关闭并释放库存：
 * - ORDPEXCLOSE202606020004：PAID_VERIFIED / LOW / 本地 SUCCESS / 只允许 CONFIRM_PAID
 * - ORDPEXCLOSE202606020005：AMOUNT_MISMATCH / HIGH / 不允许任何直接动作，转对账
 * - ORDPEXCLOSE202606020006：CALLBACK_VERIFY_FAILED / HIGH / 不允许任何直接动作，转支付排查/对账
 * - ORDPEXCLOSE202606020007：CHANNEL_UNKNOWN / HIGH / 本地 PAYING，渠道 PAYING / 不允许关闭释放
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

SET @sku_id := 900002;
SET @spu_id := 900002;
SET @user_id := 100002;
SET @now := NOW();

-- 1. 清理历史测试数据，保证可重复执行
DELETE FROM pay_reconciliation_record WHERE order_no LIKE 'ORDPEXCLOSE20260602%';
DELETE FROM pay_callback_record WHERE order_no LIKE 'ORDPEXCLOSE20260602%' OR out_trade_no LIKE 'PAYPEXCLOSE20260602%' OR pay_order_no LIKE 'PAYPEXCLOSE20260602%';
DELETE FROM pay_refund_order WHERE order_no LIKE 'ORDPEXCLOSE20260602%' OR pay_order_no LIKE 'PAYPEXCLOSE20260602%';
DELETE FROM pay_order WHERE order_no LIKE 'ORDPEXCLOSE20260602%' OR pay_order_no LIKE 'PAYPEXCLOSE20260602%';
DELETE FROM ims_stock_operation_log WHERE business_type = 'ORDER' AND business_no LIKE 'ORDPEXCLOSE20260602%';
DELETE FROM ims_stock_lock WHERE business_type = 'ORDER' AND business_no LIKE 'ORDPEXCLOSE20260602%';
DELETE FROM oms_order_item WHERE order_no LIKE 'ORDPEXCLOSE20260602%';
DELETE FROM oms_order WHERE order_no LIKE 'ORDPEXCLOSE20260602%';
DELETE FROM ums_user_address WHERE user_id = @user_id AND receiver_phone = '13900001000';
DELETE FROM ums_user WHERE id = @user_id;

-- 2. 测试用户和库存
INSERT INTO ums_user (
  id, mobile, password_hash, nickname, avatar_url, status, version, created_at, updated_at, deleted_at
) VALUES (
  @user_id, '13900001000', '$2a$10$paymentExceptionCloseReleaseTestHash', '关闭释放测试用户', '', 'ENABLED', 0, @now, @now, NULL
);

INSERT INTO ums_user_address (
  user_id, receiver_name, receiver_phone, province_code, province_name, city_code, city_name, district_code, district_name, detail_address, postal_code, is_default, version, created_at, updated_at, deleted_at
) VALUES (
  @user_id, '关闭释放测试用户', '13900001000', '', '浙江省', '', '杭州市', '', '西湖区', '关闭释放测试地址 1 号', '', 1, 0, @now, @now, NULL
);

INSERT INTO ims_stock (
  sku_id, total_stock, locked_stock, available_stock, stock_status, low_stock_threshold, high_stock_threshold, warning_status, version, created_at, updated_at
) VALUES (
  @sku_id, 1000, 0, 1000, 'ACTIVE', 10, 1000, 'NORMAL', 0, @now, @now
) ON DUPLICATE KEY UPDATE
  stock_status = 'ACTIVE',
  updated_at = @now;

-- 3. 插入支付异常订单
INSERT INTO oms_order (
  order_no, user_id, order_status, total_amount_cent, pay_amount_cent, freight_amount_cent, discount_amount_cent,
  receiver_name, receiver_phone, receiver_province_name, receiver_city_name, receiver_district_name, receiver_detail_address,
  remark, pay_type, paid_at, cancelled_at, shipped_at, completed_at, expire_time, user_deleted, user_deleted_at, version, created_at, updated_at, deleted_at
) VALUES
-- 001：无支付单，允许关闭释放
('ORDPEXCLOSE202606020001', @user_id, 'PAYMENT_EXCEPTION', 8800, 8800, 0, 0, '无单可关', '13900001001', '浙江省', '杭州市', '西湖区', '关闭释放测试地址 001', 'PAYMENT_EXCEPTION_FROM_PENDING_PAYMENT: 无支付单，测试允许关闭并释放库存', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
-- 002：支付单关闭，渠道关闭，允许关闭释放
('ORDPEXCLOSE202606020002', @user_id, 'PAYMENT_EXCEPTION', 9900, 9900, 0, 0, '关闭可关', '13900001002', '浙江省', '杭州市', '西湖区', '关闭释放测试地址 002', 'PAYMENT_EXCEPTION_FROM_PENDING_PAYMENT: 支付单关闭，渠道未支付，测试允许关闭并释放库存', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
-- 003：支付失败，渠道失败，允许关闭释放
('ORDPEXCLOSE202606020003', @user_id, 'PAYMENT_EXCEPTION', 10900, 10900, 0, 0, '失败可关', '13900001003', '浙江省', '杭州市', '西湖区', '关闭释放测试地址 003', 'PAYMENT_EXCEPTION_FROM_PENDING_PAYMENT: 支付失败，渠道未支付，测试允许关闭并释放库存', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
-- 004：本地成功，禁止关闭释放，只允许确认已支付
('ORDPEXCLOSE202606020004', @user_id, 'PAYMENT_EXCEPTION', 11900, 11900, 0, 0, '已付禁关', '13900001004', '浙江省', '杭州市', '西湖区', '关闭释放测试地址 004', 'PAYMENT_EXCEPTION_FROM_PENDING_PAYMENT: 本地支付成功，测试禁止关闭并释放库存', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
-- 005：金额不一致，禁止关闭释放
('ORDPEXCLOSE202606020005', @user_id, 'PAYMENT_EXCEPTION', 12900, 12900, 0, 0, '差额禁关', '13900001005', '浙江省', '杭州市', '西湖区', '关闭释放测试地址 005', 'PAYMENT_EXCEPTION_FROM_PENDING_PAYMENT: 金额不一致，测试禁止关闭并释放库存', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
-- 006：验签失败，禁止关闭释放
('ORDPEXCLOSE202606020006', @user_id, 'PAYMENT_EXCEPTION', 13900, 13900, 0, 0, '验签禁关', '13900001006', '浙江省', '杭州市', '西湖区', '关闭释放测试地址 006', 'PAYMENT_EXCEPTION_FROM_PENDING_PAYMENT: 回调验签失败，测试禁止关闭并释放库存', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
-- 007：支付处理中/渠道未知，禁止关闭释放
('ORDPEXCLOSE202606020007', @user_id, 'PAYMENT_EXCEPTION', 14900, 14900, 0, 0, '处理中禁关', '13900001007', '浙江省', '杭州市', '西湖区', '关闭释放测试地址 007', 'PAYMENT_EXCEPTION_FROM_PENDING_PAYMENT: 支付仍处理中，测试禁止关闭并释放库存', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL);

-- 4. 订单项闭环
INSERT INTO oms_order_item (
  order_id, order_no, sku_id, spu_id, sku_name, sku_image_url, sale_price_cent, quantity, total_amount_cent, created_at, updated_at, deleted_at
)
SELECT id, order_no, @sku_id, @spu_id, CONCAT('关闭释放专项测试商品-', RIGHT(order_no, 3)), '', pay_amount_cent, 1, pay_amount_cent, @now, @now, NULL
FROM oms_order
WHERE order_no LIKE 'ORDPEXCLOSE20260602%';

-- 5. 库存锁闭环
INSERT INTO ims_stock_lock (
  lock_no, sku_id, business_type, business_no, quantity, status, lock_time, release_time, deduct_time, reserved_synced, cancelled_synced, confirmed_synced, created_at, updated_at
)
SELECT CONCAT('LOCK-', order_no), @sku_id, 'ORDER', order_no, 1, 'RESERVED', @now, NULL, NULL, 1, 0, 0, @now, @now
FROM oms_order
WHERE order_no LIKE 'ORDPEXCLOSE20260602%';

-- 6. 支付单数据
INSERT INTO pay_order (
  pay_order_no, order_no, user_id, pay_amount_cent, pay_status, pay_channel, transaction_no, callback_payload, notify_time, paid_at, idempotent_key, version, created_at, updated_at, deleted_at
) VALUES
-- 001 故意不插入支付单
('PAYPEXCLOSE202606020002', 'ORDPEXCLOSE202606020002', @user_id, 9900, 'CLOSED', 'MOCK', '', '{"mock":"closed unpaid"}', NULL, NULL, 'PAY:MOCK:ORDPEXCLOSE202606020002', 0, @now, @now, NULL),
('PAYPEXCLOSE202606020003', 'ORDPEXCLOSE202606020003', @user_id, 10900, 'FAILED', 'MOCK', '', '{"mock":"failed unpaid"}', NULL, NULL, 'PAY:MOCK:ORDPEXCLOSE202606020003', 0, @now, @now, NULL),
('PAYPEXCLOSE202606020004', 'ORDPEXCLOSE202606020004', @user_id, 11900, 'SUCCESS', 'MOCK', 'MOCK-TXN-CLOSE-0004', '{"mock":"success paid"}', @now, @now, 'PAY:MOCK:ORDPEXCLOSE202606020004', 0, @now, @now, NULL),
('PAYPEXCLOSE202606020005', 'ORDPEXCLOSE202606020005', @user_id, 12800, 'SUCCESS', 'MOCK', 'MOCK-TXN-CLOSE-0005', '{"mock":"amount mismatch"}', @now, @now, 'PAY:MOCK:ORDPEXCLOSE202606020005', 0, @now, @now, NULL),
('PAYPEXCLOSE202606020006', 'ORDPEXCLOSE202606020006', @user_id, 13900, 'SUCCESS', 'MOCK', 'MOCK-TXN-CLOSE-0006', '{"mock":"verify failed"}', @now, @now, 'PAY:MOCK:ORDPEXCLOSE202606020006', 0, @now, @now, NULL),
('PAYPEXCLOSE202606020007', 'ORDPEXCLOSE202606020007', @user_id, 14900, 'PAYING', 'MOCK', '', '{"mock":"paying unknown"}', NULL, NULL, 'PAY:MOCK:ORDPEXCLOSE202606020007', 0, @now, @now, NULL);

-- 7. 回调记录
INSERT INTO pay_callback_record (
  channel, callback_type, pay_order_no, refund_no, order_no, out_trade_no, transaction_no, amount_cent, trade_status, signature, verified, process_status, fail_reason, raw_payload, callback_time, processed_at, created_at, updated_at
) VALUES
('MOCK', 'PAY', 'PAYPEXCLOSE202606020002', NULL, 'ORDPEXCLOSE202606020002', 'PAYPEXCLOSE202606020002', '', 9900, 'CLOSED', 'mock-signature', 1, 'IGNORED_NON_SUCCESS', '测试数据：支付单关闭，渠道未支付', '{"case":"close_release_allowed_closed"}', @now, @now, @now, @now),
('MOCK', 'PAY', 'PAYPEXCLOSE202606020003', NULL, 'ORDPEXCLOSE202606020003', 'PAYPEXCLOSE202606020003', '', 10900, 'FAILED', 'mock-signature', 1, 'IGNORED_NON_SUCCESS', '测试数据：支付失败，渠道未支付', '{"case":"close_release_allowed_failed"}', @now, @now, @now, @now),
-- 004 不插入回调，用于验证本地 SUCCESS 即禁止关闭释放，但允许确认已支付
('MOCK', 'PAY', 'PAYPEXCLOSE202606020005', NULL, 'ORDPEXCLOSE202606020005', 'PAYPEXCLOSE202606020005', 'MOCK-TXN-CLOSE-0005', 12800, 'SUCCESS', 'mock-signature', 1, 'AMOUNT_MISMATCH', '测试数据：支付金额与订单金额不一致', '{"case":"close_release_block_amount_mismatch"}', @now, @now, @now, @now),
('MOCK', 'PAY', 'PAYPEXCLOSE202606020006', NULL, 'ORDPEXCLOSE202606020006', 'PAYPEXCLOSE202606020006', 'MOCK-TXN-CLOSE-0006', 13900, 'SUCCESS', 'bad-signature', 0, 'VERIFY_FAILED', '测试数据：模拟回调验签失败', '{"case":"close_release_block_verify_failed"}', @now, @now, @now, @now),
('MOCK', 'PAY', 'PAYPEXCLOSE202606020007', NULL, 'ORDPEXCLOSE202606020007', 'PAYPEXCLOSE202606020007', '', 14900, 'PAYING', 'mock-signature', 1, 'RECEIVED', '测试数据：支付仍处理中', '{"case":"close_release_block_paying"}', @now, NULL, @now, @now);

-- 8. 高风险对账记录闭环
INSERT INTO pay_reconciliation_record (
  biz_type, order_no, pay_order_no, refund_no, local_status, channel_status, local_amount_cent, channel_amount_cent, consistent, diff_type, repair_status, remark, repaired_at, created_at, updated_at
) VALUES
('PAY', 'ORDPEXCLOSE202606020005', 'PAYPEXCLOSE202606020005', NULL, 'PAYMENT_EXCEPTION/SUCCESS', 'SUCCESS', 12900, 12800, 0, 'AMOUNT_MISMATCH', 'PENDING', '专项测试：金额不一致，禁止关闭释放库存', NULL, @now, @now),
('PAY', 'ORDPEXCLOSE202606020006', 'PAYPEXCLOSE202606020006', NULL, 'PAYMENT_EXCEPTION/SUCCESS', 'SUCCESS', 13900, 13900, 0, 'CALLBACK_VERIFY_FAILED', 'PENDING', '专项测试：回调验签失败，禁止关闭释放库存', NULL, @now, @now),
('PAY', 'ORDPEXCLOSE202606020007', 'PAYPEXCLOSE202606020007', NULL, 'PAYMENT_EXCEPTION/PAYING', 'PAYING', 14900, 14900, 0, 'CHANNEL_UNKNOWN', 'PENDING', '专项测试：支付处理中/渠道未知，禁止关闭释放库存', NULL, @now, @now);

SET FOREIGN_KEY_CHECKS = 1;

-- 9. 执行后检查数据
SELECT
  o.order_no,
  o.order_status,
  o.pay_amount_cent AS order_amount_cent,
  p.pay_order_no,
  COALESCE(p.pay_status, 'NO_PAY_ORDER') AS pay_status,
  p.pay_amount_cent AS pay_amount_cent,
  COALESCE(c.process_status, 'NO_CALLBACK') AS latest_callback_status,
  c.trade_status AS latest_trade_status,
  CASE
    WHEN o.order_no = 'ORDPEXCLOSE202606020001' THEN '预期：NO_PAY_ORDER，允许关闭释放库存'
    WHEN o.order_no = 'ORDPEXCLOSE202606020002' THEN '预期：UNPAID_VERIFIED，允许关闭释放库存'
    WHEN o.order_no = 'ORDPEXCLOSE202606020003' THEN '预期：UNPAID_VERIFIED，允许关闭释放库存'
    WHEN o.order_no = 'ORDPEXCLOSE202606020004' THEN '预期：PAID_VERIFIED，禁止关闭释放库存，只允许确认已支付'
    WHEN o.order_no = 'ORDPEXCLOSE202606020005' THEN '预期：AMOUNT_MISMATCH，禁止关闭释放库存'
    WHEN o.order_no = 'ORDPEXCLOSE202606020006' THEN '预期：CALLBACK_VERIFY_FAILED，禁止关闭释放库存'
    WHEN o.order_no = 'ORDPEXCLOSE202606020007' THEN '预期：CHANNEL_UNKNOWN，禁止关闭释放库存'
  END AS expected_result
FROM oms_order o
LEFT JOIN pay_order p ON p.order_no = o.order_no
LEFT JOIN pay_callback_record c ON c.out_trade_no = p.pay_order_no
WHERE o.order_no LIKE 'ORDPEXCLOSE20260602%'
ORDER BY o.order_no;
