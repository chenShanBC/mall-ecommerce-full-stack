/*
 * 支付异常核验测试数据补充脚本
 *
 * 用途：在已有 mall_fei 数据库基础上补充几组支付异常闭环测试数据，
 *      用于验证“支付异常处理 -> 核验支付状态 -> allowedActions 解锁”的不同结果。
 *
 * 说明：
 * 1. 本脚本只新增独立测试订单、订单项、支付单、回调记录、库存锁和可选对账记录。
 * 2. 订单号统一以 ORDPEX20260602 开头，重复执行会先清理这些测试数据，具备幂等性。
 * 3. SKU 使用 900001，脚本会补充对应库存记录，不依赖现有商品数据。
 * 4. 订单均处于 PAYMENT_EXCEPTION，用于直接进入后台“支付异常处理”弹窗核验。
 * 5. 若你的运行环境使用 Redis 库存脚本，关闭/确认库存时仍可能依赖 Redis 库存状态；本脚本主要保证 DB 侧订单、支付、回调、库存锁闭环。
 *
 * 测试订单与预期：
 * - ORDPEX202606020001：PAID_VERIFIED / LOW / 允许 CONFIRM_PAID
 * - ORDPEX202606020002：NO_PAY_ORDER / LOW / 允许 RESTORE_PENDING_PAYMENT、CLOSE_AND_RELEASE_STOCK
 * - ORDPEX202606020003：AMOUNT_MISMATCH / HIGH / 不允许直接处理，转对账
 * - ORDPEX202606020004：CALLBACK_VERIFY_FAILED / HIGH / 不允许直接处理，转对账/支付排查
 * - ORDPEX202606020005：UNPAID_VERIFIED / LOW / 允许 RESTORE_PENDING_PAYMENT、CLOSE_AND_RELEASE_STOCK
 * - ORDPEX202606020006：CHANNEL_UNKNOWN / HIGH / 不允许直接处理，保持异常
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

SET @sku_id := 900001;
SET @spu_id := 900001;
SET @user_id := 100001;
SET @now := NOW();

-- 1. 清理历史测试数据，保证脚本可重复执行
DELETE FROM pay_reconciliation_record WHERE order_no LIKE 'ORDPEX20260602%';
DELETE FROM pay_callback_record WHERE order_no LIKE 'ORDPEX20260602%' OR out_trade_no LIKE 'PAYPEX20260602%' OR pay_order_no LIKE 'PAYPEX20260602%';
DELETE FROM pay_refund_order WHERE order_no LIKE 'ORDPEX20260602%' OR pay_order_no LIKE 'PAYPEX20260602%';
DELETE FROM pay_order WHERE order_no LIKE 'ORDPEX20260602%' OR pay_order_no LIKE 'PAYPEX20260602%';
DELETE FROM ims_stock_operation_log WHERE business_type = 'ORDER' AND business_no LIKE 'ORDPEX20260602%';
DELETE FROM ims_stock_lock WHERE business_type = 'ORDER' AND business_no LIKE 'ORDPEX20260602%';
DELETE FROM oms_order_item WHERE order_no LIKE 'ORDPEX20260602%';
DELETE FROM oms_order WHERE order_no LIKE 'ORDPEX20260602%';
DELETE FROM ums_user_address WHERE user_id = @user_id AND receiver_phone = '13900000000';
DELETE FROM ums_user WHERE id = @user_id;

-- 2. 补充测试用户和库存
INSERT INTO ums_user (
  id, mobile, password_hash, nickname, avatar_url, status, version, created_at, updated_at, deleted_at
) VALUES (
  @user_id, '13900000000', '$2a$10$paymentExceptionTestHash', '支付异常测试用户', '', 'ENABLED', 0, @now, @now, NULL
);

INSERT INTO ums_user_address (
  user_id, receiver_name, receiver_phone, province_code, province_name, city_code, city_name, district_code, district_name, detail_address, postal_code, is_default, version, created_at, updated_at, deleted_at
) VALUES (
  @user_id, '支付异常测试用户', '13900000000', '', '浙江省', '', '杭州市', '', '西湖区', '支付异常测试地址 1 号', '', 1, 0, @now, @now, NULL
);

INSERT INTO ims_stock (
  sku_id, total_stock, locked_stock, available_stock, stock_status, low_stock_threshold, high_stock_threshold, warning_status, version, created_at, updated_at
) VALUES (
  @sku_id, 1000, 0, 1000, 'ACTIVE', 10, 1000, 'NORMAL', 0, @now, @now
) ON DUPLICATE KEY UPDATE
  total_stock = GREATEST(total_stock, locked_stock + available_stock),
  stock_status = 'ACTIVE',
  updated_at = @now;

-- 3. 插入 6 个支付异常订单
INSERT INTO oms_order (
  order_no, user_id, order_status, total_amount_cent, pay_amount_cent, freight_amount_cent, discount_amount_cent,
  receiver_name, receiver_phone, receiver_province_name, receiver_city_name, receiver_district_name, receiver_detail_address,
  remark, pay_type, paid_at, cancelled_at, shipped_at, completed_at, expire_time, user_deleted, user_deleted_at, version, created_at, updated_at, deleted_at
) VALUES
-- 已支付证据充分：本地支付单 SUCCESS，渠道 MOCK 查询也会返回 SUCCESS，金额一致；预期 PAID_VERIFIED / LOW / 允许人工确认已支付
('ORDPEX202606020001', @user_id, 'PAYMENT_EXCEPTION', 19900, 19900, 0, 0, '张核验', '13900000001', '浙江省', '杭州市', '西湖区', '支付异常测试地址 001', 'PAYMENT_EXCEPTION_FROM_PENDING_PAYMENT: 本地支付单成功，待人工确认订单已支付', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
-- 无支付单：预期 NO_PAY_ORDER / LOW / 允许恢复待支付或关闭释放库存
('ORDPEX202606020002', @user_id, 'PAYMENT_EXCEPTION', 9900, 9900, 0, 0, '李无单', '13900000002', '浙江省', '杭州市', '西湖区', '支付异常测试地址 002', 'PAYMENT_EXCEPTION_FROM_PENDING_PAYMENT: 用户未拉起支付或支付单创建失败', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
-- 金额不一致：订单 15900，支付单 15800；预期 AMOUNT_MISMATCH / HIGH / 不允许直接处理
('ORDPEX202606020003', @user_id, 'PAYMENT_EXCEPTION', 15900, 15900, 0, 0, '王差额', '13900000003', '浙江省', '杭州市', '西湖区', '支付异常测试地址 003', 'PAYMENT_EXCEPTION_FROM_PENDING_PAYMENT: 支付单金额与订单金额不一致，需转对账', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
-- 回调验签失败：本地支付单 SUCCESS 且金额一致，但最近回调 VERIFY_FAILED；预期 CALLBACK_VERIFY_FAILED / HIGH / 不允许直接处理
('ORDPEX202606020004', @user_id, 'PAYMENT_EXCEPTION', 25900, 25900, 0, 0, '赵验签', '13900000004', '浙江省', '杭州市', '西湖区', '支付异常测试地址 004', 'PAYMENT_EXCEPTION_FROM_PENDING_PAYMENT: 最近回调验签失败，需排查渠道签名', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
-- 明确未支付：本地支付单 CLOSED，MOCK 查询按 CLOSED 返回未支付；预期 UNPAID_VERIFIED / LOW / 允许恢复待支付或关闭释放库存
('ORDPEX202606020005', @user_id, 'PAYMENT_EXCEPTION', 6900, 6900, 0, 0, '周未付', '13900000005', '浙江省', '杭州市', '西湖区', '支付异常测试地址 005', 'PAYMENT_EXCEPTION_FROM_PENDING_PAYMENT: 支付单已关闭，渠道未支付', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
-- 支付处理中/未知：本地支付单 PAYING，MOCK 查询按 PAYING 返回未支付但本地仍处理中；预期 CHANNEL_UNKNOWN / HIGH / 不允许直接处理
('ORDPEX202606020006', @user_id, 'PAYMENT_EXCEPTION', 12900, 12900, 0, 0, '钱处理中', '13900000006', '浙江省', '杭州市', '西湖区', '支付异常测试地址 006', 'PAYMENT_EXCEPTION_FROM_PENDING_PAYMENT: 支付单仍处理中，需稍后重试核验', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL);

-- 4. 插入订单明细，保证订单详情闭环
INSERT INTO oms_order_item (
  order_id, order_no, sku_id, spu_id, sku_name, sku_image_url, sale_price_cent, quantity, total_amount_cent, created_at, updated_at, deleted_at
)
SELECT id, order_no, @sku_id, @spu_id, CONCAT('支付异常核验测试商品-', RIGHT(order_no, 3)), '', pay_amount_cent, 1, pay_amount_cent, @now, @now, NULL
FROM oms_order
WHERE order_no LIKE 'ORDPEX20260602%';

-- 5. 插入库存锁记录，保证关闭释放库存、人工确认库存时有业务锁记录可处理
INSERT INTO ims_stock_lock (
  lock_no, sku_id, business_type, business_no, quantity, status, lock_time, release_time, deduct_time, reserved_synced, cancelled_synced, confirmed_synced, created_at, updated_at
)
SELECT CONCAT('LOCK-', order_no), @sku_id, 'ORDER', order_no, 1, 'RESERVED', @now, NULL, NULL, 1, 0, 0, @now, @now
FROM oms_order
WHERE order_no LIKE 'ORDPEX20260602%';

-- 6. 插入支付单
INSERT INTO pay_order (
  pay_order_no, order_no, user_id, pay_amount_cent, pay_status, pay_channel, transaction_no, callback_payload, notify_time, paid_at, idempotent_key, version, created_at, updated_at, deleted_at
) VALUES
('PAYPEX202606020001', 'ORDPEX202606020001', @user_id, 19900, 'SUCCESS', 'MOCK', 'MOCK-TXN-PEX-0001', '{"mock":"paid verified"}', @now, @now, 'PAY:MOCK:ORDPEX202606020001', 0, @now, @now, NULL),
('PAYPEX202606020003', 'ORDPEX202606020003', @user_id, 15800, 'SUCCESS', 'MOCK', 'MOCK-TXN-PEX-0003', '{"mock":"amount mismatch"}', @now, @now, 'PAY:MOCK:ORDPEX202606020003', 0, @now, @now, NULL),
('PAYPEX202606020004', 'ORDPEX202606020004', @user_id, 25900, 'SUCCESS', 'MOCK', 'MOCK-TXN-PEX-0004', '{"mock":"callback verify failed"}', @now, @now, 'PAY:MOCK:ORDPEX202606020004', 0, @now, @now, NULL),
('PAYPEX202606020005', 'ORDPEX202606020005', @user_id, 6900, 'CLOSED', 'MOCK', '', '{"mock":"closed unpaid"}', NULL, NULL, 'PAY:MOCK:ORDPEX202606020005', 0, @now, @now, NULL),
('PAYPEX202606020006', 'ORDPEX202606020006', @user_id, 12900, 'PAYING', 'MOCK', '', '{"mock":"paying unknown"}', NULL, NULL, 'PAY:MOCK:ORDPEX202606020006', 0, @now, @now, NULL);

-- 7. 插入回调记录
INSERT INTO pay_callback_record (
  channel, callback_type, pay_order_no, refund_no, order_no, out_trade_no, transaction_no, amount_cent, trade_status, signature, verified, process_status, fail_reason, raw_payload, callback_time, processed_at, created_at, updated_at
) VALUES
-- 订单 001 不插入回调，用于验证 NO_CALLBACK 但支付成功证据充分
('MOCK', 'PAY', 'PAYPEX202606020003', NULL, 'ORDPEX202606020003', 'PAYPEX202606020003', 'MOCK-TXN-PEX-0003', 15800, 'SUCCESS', 'mock-signature', 1, 'AMOUNT_MISMATCH', '支付金额与订单金额不一致：pay=15800, order=15900', '{"case":"amount_mismatch"}', @now, @now, @now, @now),
('MOCK', 'PAY', 'PAYPEX202606020004', NULL, 'ORDPEX202606020004', 'PAYPEX202606020004', 'MOCK-TXN-PEX-0004', 25900, 'SUCCESS', 'bad-signature', 0, 'VERIFY_FAILED', '测试数据：模拟回调验签失败', '{"case":"verify_failed"}', @now, @now, @now, @now),
('MOCK', 'PAY', 'PAYPEX202606020005', NULL, 'ORDPEX202606020005', 'PAYPEX202606020005', '', 6900, 'CLOSED', 'mock-signature', 1, 'IGNORED_NON_SUCCESS', '测试数据：渠道未支付/已关闭', '{"case":"unpaid_closed"}', @now, @now, @now, @now),
('MOCK', 'PAY', 'PAYPEX202606020006', NULL, 'ORDPEX202606020006', 'PAYPEX202606020006', '', 12900, 'PAYING', 'mock-signature', 1, 'RECEIVED', '测试数据：支付仍处理中', '{"case":"paying_unknown"}', @now, NULL, @now, @now);

-- 8. 插入对账记录，用于高风险场景跳转/查看闭环
INSERT INTO pay_reconciliation_record (
  biz_type, order_no, pay_order_no, refund_no, local_status, channel_status, local_amount_cent, channel_amount_cent, consistent, diff_type, repair_status, remark, repaired_at, created_at, updated_at
) VALUES
('PAY', 'ORDPEX202606020003', 'PAYPEX202606020003', NULL, 'PAYMENT_EXCEPTION/SUCCESS', 'SUCCESS', 15900, 15800, 0, 'AMOUNT_MISMATCH', 'PENDING', '测试数据：订单金额与支付单金额不一致，需对账处理', NULL, @now, @now),
('PAY', 'ORDPEX202606020004', 'PAYPEX202606020004', NULL, 'PAYMENT_EXCEPTION/SUCCESS', 'SUCCESS', 25900, 25900, 0, 'CALLBACK_VERIFY_FAILED', 'PENDING', '测试数据：支付回调验签失败，需核查渠道签名', NULL, @now, @now),
('PAY', 'ORDPEX202606020006', 'PAYPEX202606020006', NULL, 'PAYMENT_EXCEPTION/PAYING', 'PAYING', 12900, 12900, 0, 'CHANNEL_UNKNOWN', 'PENDING', '测试数据：支付仍处理中，需稍后重试核验', NULL, @now, @now);

SET FOREIGN_KEY_CHECKS = 1;

-- 9. 执行后可用下面查询检查测试数据
SELECT
  o.order_no,
  o.order_status,
  o.pay_amount_cent AS order_amount_cent,
  p.pay_order_no,
  p.pay_status,
  p.pay_amount_cent AS pay_amount_cent,
  c.process_status AS latest_callback_status,
  c.trade_status AS latest_trade_status,
  o.remark
FROM oms_order o
LEFT JOIN pay_order p ON p.order_no = o.order_no
LEFT JOIN pay_callback_record c ON c.out_trade_no = p.pay_order_no
WHERE o.order_no LIKE 'ORDPEX20260602%'
ORDER BY o.order_no;
