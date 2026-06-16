/*
 * 支付管理页「同步状态 / 补偿订单」验证数据
 *
 * 用途：专门验证后台支付管理页两个按钮：
 * 1. 同步状态：POST /api/admin/pays/{orderNo}/sync-status
 * 2. 补偿订单：POST /api/admin/pays/{orderNo}/repair-paid
 *
 * 订单号前缀：ORDPAYSYNC20260602
 * 支付单前缀：PAYPAYSYNC20260602
 * SKU：900003
 * 用户：100003
 *
 * 重要说明：
 * 1. 当前 MOCK 渠道支持通过 callback_payload 中的 mockChannelStatus=SUCCESS 模拟“渠道真实状态已支付”。
 * 2. 因此本脚本提供的“同步状态”验证数据是真正的本地/渠道不一致：本地支付单 PAYING，MOCK 渠道查询 SUCCESS。
 * 3. 点击同步状态后，后端会主动查询 MOCK 渠道，将本地支付单修正为 SUCCESS，并推进订单为已支付。
 * 4. 如果要验证真实支付宝链路，可将支付渠道换成 ALIPAY_PC / ALIPAY_WAP 并配置真实沙箱交易。
 * 5. 本脚本补齐订单、订单项、库存、库存锁、支付单、回调记录、对账记录，可重复执行。
 *
 * 验证场景：
 *
 * A. ORDPAYSYNC202606020001：验证「同步状态」
 *    - 初始订单：PENDING_PAYMENT
 *    - 初始本地支付单：PAYING
 *    - 模拟渠道状态：SUCCESS（callback_payload.mockChannelStatus）
 *    - 库存锁：RESERVED
 *    - 操作：后台支付管理页搜索该订单 -> 点击“同步状态”
 *    - 预期：支付单从 PAYING 修正为 SUCCESS，订单被 markPaid，订单状态推进为已支付/待发货类状态
 *
 * B. ORDPAYSYNC202606020002：验证「补偿订单」
 *    - 初始订单：PENDING_PAYMENT
 *    - 初始支付单：SUCCESS
 *    - 库存锁：RESERVED
 *    - 操作：后台支付管理页搜索该订单 -> 点击“补偿订单”
 *    - 预期：订单被 markPaid，订单状态推进为已支付/待发货类状态；支付单仍为 SUCCESS
 *
 * C. ORDPAYSYNC202606020003：验证「补偿订单失败保护」
 *    - 初始订单：PENDING_PAYMENT
 *    - 初始支付单：PAYING
 *    - 操作：点击“补偿订单”
 *    - 预期：按钮前端应禁用；如直接调接口，后端返回“当前支付单尚未成功，无法补偿订单支付状态”
 *
 * D. ORDPAYSYNC202606020004：验证「已支付订单幂等保护」
 *    - 初始订单：PAID
 *    - 初始支付单：SUCCESS
 *    - 操作：点击“同步状态”或“补偿订单”
 *    - 预期：不会重复推进，接口幂等返回支付单详情
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

SET @pay_sync_sku_id := 900003;
SET @pay_sync_spu_id := 900003;
SET @pay_sync_user_id := 100003;
SET @now := NOW();

-- 1. 清理历史测试数据，保证可重复执行
DELETE FROM pay_reconciliation_record WHERE order_no LIKE 'ORDPAYSYNC20260602%';
DELETE FROM pay_callback_record WHERE order_no LIKE 'ORDPAYSYNC20260602%' OR out_trade_no LIKE 'PAYPAYSYNC20260602%' OR pay_order_no LIKE 'PAYPAYSYNC20260602%';
DELETE FROM pay_refund_order WHERE order_no LIKE 'ORDPAYSYNC20260602%' OR pay_order_no LIKE 'PAYPAYSYNC20260602%';
DELETE FROM pay_order WHERE order_no LIKE 'ORDPAYSYNC20260602%' OR pay_order_no LIKE 'PAYPAYSYNC20260602%';
DELETE FROM ims_stock_operation_log WHERE business_type = 'ORDER' AND business_no LIKE 'ORDPAYSYNC20260602%';
DELETE FROM ims_stock_lock WHERE business_type = 'ORDER' AND business_no LIKE 'ORDPAYSYNC20260602%';
DELETE FROM oms_order_item WHERE order_no LIKE 'ORDPAYSYNC20260602%';
DELETE FROM oms_order WHERE order_no LIKE 'ORDPAYSYNC20260602%';
DELETE FROM ums_user_address WHERE user_id = @pay_sync_user_id AND receiver_phone = '13900002000';
DELETE FROM ums_user WHERE id = @pay_sync_user_id;

-- 2. 测试用户和地址
INSERT INTO ums_user (
  id, mobile, password_hash, nickname, avatar_url, status, version, created_at, updated_at, deleted_at
) VALUES (
  @pay_sync_user_id, '13900002000', '$2a$10$paySyncRepairTestHash', '支付同步补偿测试用户', '', 'ENABLED', 0, @now, @now, NULL
);

INSERT INTO ums_user_address (
  user_id, receiver_name, receiver_phone, province_code, province_name, city_code, city_name, district_code, district_name, detail_address, postal_code, is_default, version, created_at, updated_at, deleted_at
) VALUES (
  @pay_sync_user_id, '支付同步补偿测试用户', '13900002000', '', '浙江省', '', '杭州市', '', '西湖区', '支付同步补偿测试地址 1 号', '', 1, 0, @now, @now, NULL
);

-- 3. 库存主数据：4 个测试订单各锁 1 件，因此 locked_stock=4，available_stock=996
INSERT INTO ims_stock (
  sku_id, total_stock, locked_stock, available_stock, stock_status, low_stock_threshold, high_stock_threshold, warning_status, version, created_at, updated_at
) VALUES (
  @pay_sync_sku_id, 1000, 4, 996, 'ACTIVE', 10, 1000, 'NORMAL', 0, @now, @now
) ON DUPLICATE KEY UPDATE
  total_stock = 1000,
  locked_stock = 4,
  available_stock = 996,
  stock_status = 'ACTIVE',
  warning_status = 'NORMAL',
  updated_at = @now;

-- 4. 订单主数据
INSERT INTO oms_order (
  order_no, user_id, order_status, total_amount_cent, pay_amount_cent, freight_amount_cent, discount_amount_cent,
  receiver_name, receiver_phone, receiver_province_name, receiver_city_name, receiver_district_name, receiver_detail_address,
  remark, pay_type, paid_at, cancelled_at, shipped_at, completed_at, expire_time, user_deleted, user_deleted_at, version, created_at, updated_at, deleted_at
) VALUES
('ORDPAYSYNC202606020001', @pay_sync_user_id, 'PENDING_PAYMENT', 8800, 8800, 0, 0, '同步状态测试', '13900002001', '浙江省', '杭州市', '西湖区', '支付同步测试地址 001', 'PAY_SYNC_TEST: 本地支付单 PAYING，但模拟渠道查询 SUCCESS，用于验证同步状态按钮修正支付单并推进订单', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 7 DAY), 0, NULL, 0, @now, @now, NULL),
('ORDPAYSYNC202606020002', @pay_sync_user_id, 'PENDING_PAYMENT', 9900, 9900, 0, 0, '补偿订单测试', '13900002002', '浙江省', '杭州市', '西湖区', '支付补偿测试地址 002', 'PAY_REPAIR_TEST: 支付单已成功但订单仍待支付，用于验证补偿订单按钮', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
('ORDPAYSYNC202606020003', @pay_sync_user_id, 'PENDING_PAYMENT', 6600, 6600, 0, 0, '补偿失败测试', '13900002003', '浙江省', '杭州市', '西湖区', '支付补偿失败测试地址 003', 'PAY_REPAIR_GUARD_TEST: 支付单仍处理中，用于验证补偿订单失败保护', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
('ORDPAYSYNC202606020004', @pay_sync_user_id, 'PAID', 7700, 7700, 0, 0, '幂等保护测试', '13900002004', '浙江省', '杭州市', '西湖区', '支付幂等测试地址 004', 'PAY_IDEMPOTENT_TEST: 订单已支付且支付单成功，用于验证重复同步/补偿幂等', 'MOCK', @now, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL);

-- 5. 订单项
INSERT INTO oms_order_item (
  order_id, order_no, sku_id, spu_id, sku_name, sku_image_url, sale_price_cent, quantity, total_amount_cent, created_at, updated_at, deleted_at
)
SELECT id, order_no, @pay_sync_sku_id, @pay_sync_spu_id, CONCAT('支付同步补偿测试商品-', RIGHT(order_no, 3)), '', pay_amount_cent, 1, pay_amount_cent, @now, @now, NULL
FROM oms_order
WHERE order_no LIKE 'ORDPAYSYNC20260602%';

-- 6. 库存锁记录：待支付/已支付场景都保留 RESERVED，供订单支付后库存确认链路使用
INSERT INTO ims_stock_lock (
  lock_no, sku_id, business_type, business_no, quantity, status, lock_time, release_time, deduct_time, reserved_synced, cancelled_synced, confirmed_synced, created_at, updated_at
)
SELECT CONCAT('LOCK-', order_no), @pay_sync_sku_id, 'ORDER', order_no, 1, 'RESERVED', @now, NULL, NULL, 1, 0, CASE WHEN order_status = 'PAID' THEN 1 ELSE 0 END, @now, @now
FROM oms_order
WHERE order_no LIKE 'ORDPAYSYNC20260602%';

-- 7. 库存操作日志：补齐业务锁定记录；已支付幂等订单额外给一条确认日志
INSERT INTO ims_stock_operation_log (
  sku_id, operation_type, business_type, business_no, change_quantity,
  before_total_stock, before_locked_stock, before_available_stock,
  after_total_stock, after_locked_stock, after_available_stock,
  remark, operator_type, operator_id, operator_name, source_type, created_at
) VALUES
(@pay_sync_sku_id, 'LOCK', 'ORDER', 'ORDPAYSYNC202606020001', 1, 1000, 0, 1000, 1000, 1, 999, '支付同步测试订单锁库存', 'SYSTEM', NULL, 'TEST_SQL', 'TEST_DATA', @now),
(@pay_sync_sku_id, 'LOCK', 'ORDER', 'ORDPAYSYNC202606020002', 1, 1000, 1, 999, 1000, 2, 998, '支付补偿测试订单锁库存', 'SYSTEM', NULL, 'TEST_SQL', 'TEST_DATA', @now),
(@pay_sync_sku_id, 'LOCK', 'ORDER', 'ORDPAYSYNC202606020003', 1, 1000, 2, 998, 1000, 3, 997, '支付补偿失败测试订单锁库存', 'SYSTEM', NULL, 'TEST_SQL', 'TEST_DATA', @now),
(@pay_sync_sku_id, 'LOCK', 'ORDER', 'ORDPAYSYNC202606020004', 1, 1000, 3, 997, 1000, 4, 996, '支付幂等测试订单锁库存', 'SYSTEM', NULL, 'TEST_SQL', 'TEST_DATA', @now),
(@pay_sync_sku_id, 'CONFIRM', 'ORDER', 'ORDPAYSYNC202606020004', -1, 1000, 4, 996, 999, 3, 996, '支付幂等测试订单已确认库存', 'SYSTEM', NULL, 'TEST_SQL', 'TEST_DATA', @now);

-- 8. 支付单
INSERT INTO pay_order (
  pay_order_no, order_no, user_id, pay_amount_cent, pay_status, pay_channel, transaction_no, callback_payload, notify_time, paid_at, idempotent_key, version, created_at, updated_at, deleted_at
) VALUES
('PAYPAYSYNC202606020001', 'ORDPAYSYNC202606020001', @pay_sync_user_id, 8800, 'PAYING', 'MOCK', 'MOCK-PAYSYNC-TXN-0001', '{"case":"sync_status_local_paying_channel_success","mockChannelStatus":"SUCCESS"}', NULL, NULL, 'PAY:MOCK:ORDPAYSYNC202606020001', 0, @now, @now, NULL),
('PAYPAYSYNC202606020002', 'ORDPAYSYNC202606020002', @pay_sync_user_id, 9900, 'SUCCESS', 'MOCK', 'MOCK-PAYSYNC-TXN-0002', '{"case":"repair_paid_order_pending_pay_success"}', @now, @now, 'PAY:MOCK:ORDPAYSYNC202606020002', 0, @now, @now, NULL),
('PAYPAYSYNC202606020003', 'ORDPAYSYNC202606020003', @pay_sync_user_id, 6600, 'PAYING', 'MOCK', '', '{"case":"repair_guard_pay_still_paying"}', NULL, NULL, 'PAY:MOCK:ORDPAYSYNC202606020003', 0, @now, @now, NULL),
('PAYPAYSYNC202606020004', 'ORDPAYSYNC202606020004', @pay_sync_user_id, 7700, 'SUCCESS', 'MOCK', 'MOCK-PAYSYNC-TXN-0004', '{"case":"idempotent_paid_order_success_pay"}', @now, @now, 'PAY:MOCK:ORDPAYSYNC202606020004', 0, @now, @now, NULL);

-- 9. 支付回调记录：让支付管理页回调记录也能查到配套证据
INSERT INTO pay_callback_record (
  channel, callback_type, pay_order_no, refund_no, order_no, out_trade_no, transaction_no, amount_cent, trade_status, signature, verified, process_status, fail_reason, raw_payload, callback_time, processed_at, created_at, updated_at
) VALUES
('MOCK', 'PAY', 'PAYPAYSYNC202606020001', NULL, 'ORDPAYSYNC202606020001', 'PAYPAYSYNC202606020001', 'MOCK-PAYSYNC-TXN-0001', 8800, 'SUCCESS', 'mock-signature', 1, 'RECEIVED', '测试数据：本地仍为 PAYING，但渠道真实状态已成功，等待同步状态修正', '{"case":"sync_status_channel_success_waiting_sync"}', @now, NULL, @now, @now),
('MOCK', 'PAY', 'PAYPAYSYNC202606020002', NULL, 'ORDPAYSYNC202606020002', 'PAYPAYSYNC202606020002', 'MOCK-PAYSYNC-TXN-0002', 9900, 'SUCCESS', 'mock-signature', 1, 'PROCESSED', '', '{"case":"repair_paid_processed_callback"}', @now, @now, @now, @now),
('MOCK', 'PAY', 'PAYPAYSYNC202606020003', NULL, 'ORDPAYSYNC202606020003', 'PAYPAYSYNC202606020003', '', 6600, 'PAYING', 'mock-signature', 1, 'RECEIVED', '测试数据：支付仍处理中，不能补偿订单', '{"case":"repair_guard_paying_callback"}', @now, NULL, @now, @now),
('MOCK', 'PAY', 'PAYPAYSYNC202606020004', NULL, 'ORDPAYSYNC202606020004', 'PAYPAYSYNC202606020004', 'MOCK-PAYSYNC-TXN-0004', 7700, 'SUCCESS', 'mock-signature', 1, 'PROCESSED', '', '{"case":"idempotent_processed_callback"}', @now, @now, @now, @now);

-- 10. 对账记录：用于对账管理页查看这几笔测试单的本地/渠道一致性
INSERT INTO pay_reconciliation_record (
  biz_type, order_no, pay_order_no, refund_no, local_status, channel_status, local_amount_cent, channel_amount_cent, consistent, diff_type, repair_status, remark, repaired_at, created_at, updated_at
) VALUES
('PAY', 'ORDPAYSYNC202606020001', 'PAYPAYSYNC202606020001', NULL, 'PENDING_PAYMENT/PAYING', 'SUCCESS', 8800, 8800, 0, 'LOCAL_PAYING_CHANNEL_SUCCESS', 'PENDING', '测试数据：本地支付单 PAYING 但 MOCK 渠道查询 SUCCESS，可用同步状态修正支付单并推进订单', NULL, @now, @now),
('PAY', 'ORDPAYSYNC202606020002', 'PAYPAYSYNC202606020002', NULL, 'PENDING_PAYMENT/SUCCESS', 'SUCCESS', 9900, 9900, 0, 'ORDER_STATUS_NOT_SYNCED', 'PENDING', '测试数据：支付单成功但订单仍待支付，可用补偿订单推进订单', NULL, @now, @now),
('PAY', 'ORDPAYSYNC202606020003', 'PAYPAYSYNC202606020003', NULL, 'PENDING_PAYMENT/PAYING', 'PAYING', 6600, 6600, 1, 'PAYING', 'NONE', '测试数据：支付处理中，补偿订单应被禁止', NULL, @now, @now),
('PAY', 'ORDPAYSYNC202606020004', 'PAYPAYSYNC202606020004', NULL, 'PAID/SUCCESS', 'SUCCESS', 7700, 7700, 1, 'NONE', 'DONE', '测试数据：订单已支付且支付单成功，重复同步/补偿应幂等', @now, @now, @now);

SET FOREIGN_KEY_CHECKS = 1;

-- 11. 执行后检查数据
SELECT
  o.order_no,
  o.order_status,
  o.pay_amount_cent AS order_amount_cent,
  p.pay_order_no,
  p.pay_status,
  p.pay_amount_cent AS pay_amount_cent,
  s.sku_id,
  s.total_stock,
  s.locked_stock,
  s.available_stock,
  l.lock_no,
  l.status AS stock_lock_status,
  l.quantity AS lock_quantity,
  c.process_status AS callback_process_status,
  c.trade_status AS callback_trade_status,
  CASE o.order_no
    WHEN 'ORDPAYSYNC202606020001' THEN '验证同步状态：本地 PAYING / 渠道 SUCCESS，点击同步状态后支付单应变 SUCCESS，订单应推进为已支付/待发货类状态'
    WHEN 'ORDPAYSYNC202606020002' THEN '验证补偿订单：点击补偿订单后订单应从 PENDING_PAYMENT 推进为已支付/待发货类状态'
    WHEN 'ORDPAYSYNC202606020003' THEN '验证补偿失败保护：支付单 PAYING，补偿订单应禁用或接口报错'
    WHEN 'ORDPAYSYNC202606020004' THEN '验证幂等保护：订单已支付，重复同步/补偿不应产生异常副作用'
  END AS expected_result
FROM oms_order o
JOIN pay_order p ON p.order_no = o.order_no
LEFT JOIN oms_order_item oi ON oi.order_no = o.order_no
LEFT JOIN ims_stock s ON s.sku_id = oi.sku_id
LEFT JOIN ims_stock_lock l ON l.business_type = 'ORDER' AND l.business_no = o.order_no
LEFT JOIN pay_callback_record c ON c.out_trade_no = p.pay_order_no
WHERE o.order_no LIKE 'ORDPAYSYNC20260602%'
ORDER BY o.order_no;
