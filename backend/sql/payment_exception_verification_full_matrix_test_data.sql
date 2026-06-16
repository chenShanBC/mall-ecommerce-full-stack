/*
 * 支付异常核验全覆盖测试数据补充脚本
 *
 * 用途：覆盖支付异常核验的低/中/高风险，以及三个处理按钮的解锁情况：
 * - 人工确认已支付 CONFIRM_PAID
 * - 恢复待支付 RESTORE_PENDING_PAYMENT
 * - 关闭并释放库存 CLOSE_AND_RELEASE_STOCK
 *
 * 订单号前缀：ORDPEXMATRIX20260602
 * 支付单前缀：PAYPEXMATRIX20260602
 *
 * 重要说明：
 * 1. 本脚本用于在已有 mall_fei 数据库基础上补充测试数据。
 * 2. 脚本会先清理同前缀测试数据，可重复执行。
 * 3. 所有订单均为 PAYMENT_EXCEPTION，方便直接进入“支付异常处理”弹窗核验。
 * 4. 本脚本覆盖当前代码中 decidePaymentExceptionAction 的所有主要结论：
 *    ORDER_NOT_PAYMENT_EXCEPTION 除外，因为该结论要求订单不是 PAYMENT_EXCEPTION，不属于支付异常处理弹窗的主测试场景。
 * 5. 如果需要测试 ORDER_NOT_PAYMENT_EXCEPTION，可额外把某个测试订单状态改成 PAID 后直接调用核验接口。
 * 6. DB 侧会补齐订单、订单项、支付单、回调、库存锁、对账记录，形成闭环。
 * 7. 如果你的库存服务依赖 Redis 库存脚本，执行“关闭并释放库存/人工确认已支付”时仍可能需要 Redis 中也存在对应库存锁状态；本脚本主要保证 DB 数据闭环。
 *
 * 测试矩阵：
 *
 * 订单号                     风险  结论                      人工确认  恢复待支付  关闭释放库存  场景
 * ORDPEXMATRIX202606020001   LOW   PAID_VERIFIED             可点      禁用        禁用          本地支付单成功，金额一致
 * ORDPEXMATRIX202606020002   LOW   PAID_VERIFIED             可点      禁用        禁用          渠道成功证据，最近回调成功
 * ORDPEXMATRIX202606020003   LOW   NO_PAY_ORDER              禁用      可点        可点          无支付单
 * ORDPEXMATRIX202606020004   LOW   UNPAID_VERIFIED           禁用      可点        可点          支付单关闭，渠道未支付
 * ORDPEXMATRIX202606020005   MED   LOCAL_UNPAID              禁用      可点        可点          支付单失败，使用不可路由渠道模拟渠道未知
 * ORDPEXMATRIX202606020006   HIGH  AMOUNT_MISMATCH           禁用      禁用        禁用          金额不一致
 * ORDPEXMATRIX202606020007   HIGH  CALLBACK_VERIFY_FAILED    禁用      禁用        禁用          回调验签失败
 * ORDPEXMATRIX202606020008   HIGH  BUSINESS_MISMATCH         禁用      禁用        禁用          回调业务不匹配
 * ORDPEXMATRIX202606020009   HIGH  CHANNEL_UNKNOWN           禁用      禁用        禁用          支付中/渠道未知
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

SET @matrix_sku_id := 900002;
SET @matrix_spu_id := 900002;
SET @matrix_user_id := 100002;
SET @now := NOW();

-- 1. 清理历史测试数据
DELETE FROM pay_reconciliation_record WHERE order_no LIKE 'ORDPEXMATRIX20260602%';
DELETE FROM pay_callback_record WHERE order_no LIKE 'ORDPEXMATRIX20260602%' OR out_trade_no LIKE 'PAYPEXMATRIX20260602%' OR pay_order_no LIKE 'PAYPEXMATRIX20260602%';
DELETE FROM pay_refund_order WHERE order_no LIKE 'ORDPEXMATRIX20260602%' OR pay_order_no LIKE 'PAYPEXMATRIX20260602%';
DELETE FROM pay_order WHERE order_no LIKE 'ORDPEXMATRIX20260602%' OR pay_order_no LIKE 'PAYPEXMATRIX20260602%';
DELETE FROM ims_stock_operation_log WHERE business_type = 'ORDER' AND business_no LIKE 'ORDPEXMATRIX20260602%';
DELETE FROM ims_stock_lock WHERE business_type = 'ORDER' AND business_no LIKE 'ORDPEXMATRIX20260602%';
DELETE FROM oms_order_item WHERE order_no LIKE 'ORDPEXMATRIX20260602%';
DELETE FROM oms_order WHERE order_no LIKE 'ORDPEXMATRIX20260602%';
DELETE FROM ums_user_address WHERE user_id = @matrix_user_id AND receiver_phone = '13900001000';
DELETE FROM ums_user WHERE id = @matrix_user_id;

-- 2. 测试用户、地址、库存
INSERT INTO ums_user (
  id, mobile, password_hash, nickname, avatar_url, status, version, created_at, updated_at, deleted_at
) VALUES (
  @matrix_user_id, '13900001000', '$2a$10$paymentExceptionMatrixHash', '支付异常矩阵测试用户', '', 'ENABLED', 0, @now, @now, NULL
);

INSERT INTO ums_user_address (
  user_id, receiver_name, receiver_phone, province_code, province_name, city_code, city_name, district_code, district_name, detail_address, postal_code, is_default, version, created_at, updated_at, deleted_at
) VALUES (
  @matrix_user_id, '支付异常矩阵测试用户', '13900001000', '', '浙江省', '', '杭州市', '', '西湖区', '支付异常矩阵测试地址 1 号', '', 1, 0, @now, @now, NULL
);

INSERT INTO ims_stock (
  sku_id, total_stock, locked_stock, available_stock, stock_status, low_stock_threshold, high_stock_threshold, warning_status, version, created_at, updated_at
) VALUES (
  @matrix_sku_id, 2000, 0, 2000, 'ACTIVE', 10, 2000, 'NORMAL', 0, @now, @now
) ON DUPLICATE KEY UPDATE
  total_stock = GREATEST(total_stock, locked_stock + available_stock),
  stock_status = 'ACTIVE',
  updated_at = @now;

-- 3. 订单主数据
INSERT INTO oms_order (
  order_no, user_id, order_status, total_amount_cent, pay_amount_cent, freight_amount_cent, discount_amount_cent,
  receiver_name, receiver_phone, receiver_province_name, receiver_city_name, receiver_district_name, receiver_detail_address,
  remark, pay_type, paid_at, cancelled_at, shipped_at, completed_at, expire_time, user_deleted, user_deleted_at, version, created_at, updated_at, deleted_at
) VALUES
('ORDPEXMATRIX202606020001', @matrix_user_id, 'PAYMENT_EXCEPTION', 10100, 10100, 0, 0, '矩阵已付一', '13900001001', '浙江省', '杭州市', '西湖区', '矩阵测试地址 001', 'MATRIX: PAID_VERIFIED，本地支付单 SUCCESS，人工确认已支付按钮应可点', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
('ORDPEXMATRIX202606020002', @matrix_user_id, 'PAYMENT_EXCEPTION', 10200, 10200, 0, 0, '矩阵已付二', '13900001002', '浙江省', '杭州市', '西湖区', '矩阵测试地址 002', 'MATRIX: PAID_VERIFIED，回调已处理成功，人工确认已支付按钮应可点', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
('ORDPEXMATRIX202606020003', @matrix_user_id, 'PAYMENT_EXCEPTION', 10300, 10300, 0, 0, '矩阵无单', '13900001003', '浙江省', '杭州市', '西湖区', '矩阵测试地址 003', 'MATRIX: NO_PAY_ORDER，恢复待支付和关闭释放库存应可点', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
('ORDPEXMATRIX202606020004', @matrix_user_id, 'PAYMENT_EXCEPTION', 10400, 10400, 0, 0, '矩阵关闭', '13900001004', '浙江省', '杭州市', '西湖区', '矩阵测试地址 004', 'MATRIX: UNPAID_VERIFIED，支付单 CLOSED，恢复待支付和关闭释放库存应可点', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
('ORDPEXMATRIX202606020005', @matrix_user_id, 'PAYMENT_EXCEPTION', 10500, 10500, 0, 0, '矩阵失败', '13900001005', '浙江省', '杭州市', '西湖区', '矩阵测试地址 005', 'MATRIX: LOCAL_UNPAID，支付单 FAILED 且渠道不可确认，中风险，恢复待支付和关闭释放库存当前实现应可点', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
('ORDPEXMATRIX202606020006', @matrix_user_id, 'PAYMENT_EXCEPTION', 10600, 10600, 0, 0, '矩阵差额', '13900001006', '浙江省', '杭州市', '西湖区', '矩阵测试地址 006', 'MATRIX: AMOUNT_MISMATCH，高风险，三个按钮均禁用', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
('ORDPEXMATRIX202606020007', @matrix_user_id, 'PAYMENT_EXCEPTION', 10700, 10700, 0, 0, '矩阵验签', '13900001007', '浙江省', '杭州市', '西湖区', '矩阵测试地址 007', 'MATRIX: CALLBACK_VERIFY_FAILED，高风险，三个按钮均禁用', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
('ORDPEXMATRIX202606020008', @matrix_user_id, 'PAYMENT_EXCEPTION', 10800, 10800, 0, 0, '矩阵业务错', '13900001008', '浙江省', '杭州市', '西湖区', '矩阵测试地址 008', 'MATRIX: BUSINESS_MISMATCH，高风险，三个按钮均禁用', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL),
('ORDPEXMATRIX202606020009', @matrix_user_id, 'PAYMENT_EXCEPTION', 10900, 10900, 0, 0, '矩阵处理中', '13900001009', '浙江省', '杭州市', '西湖区', '矩阵测试地址 009', 'MATRIX: CHANNEL_UNKNOWN，支付中/渠道未知，高风险，三个按钮均禁用', 'MOCK', NULL, NULL, NULL, NULL, DATE_ADD(@now, INTERVAL 30 MINUTE), 0, NULL, 0, @now, @now, NULL);

-- 4. 订单项
INSERT INTO oms_order_item (
  order_id, order_no, sku_id, spu_id, sku_name, sku_image_url, sale_price_cent, quantity, total_amount_cent, created_at, updated_at, deleted_at
)
SELECT id, order_no, @matrix_sku_id, @matrix_spu_id, CONCAT('支付异常矩阵测试商品-', RIGHT(order_no, 3)), '', pay_amount_cent, 1, pay_amount_cent, @now, @now, NULL
FROM oms_order
WHERE order_no LIKE 'ORDPEXMATRIX20260602%';

-- 5. 库存锁记录：保证订单侧库存闭环
INSERT INTO ims_stock_lock (
  lock_no, sku_id, business_type, business_no, quantity, status, lock_time, release_time, deduct_time, reserved_synced, cancelled_synced, confirmed_synced, created_at, updated_at
)
SELECT CONCAT('LOCK-', order_no), @matrix_sku_id, 'ORDER', order_no, 1, 'RESERVED', @now, NULL, NULL, 1, 0, 0, @now, @now
FROM oms_order
WHERE order_no LIKE 'ORDPEXMATRIX20260602%';

-- 6. 支付单
-- 注意：订单 003 故意不插入支付单，用于 NO_PAY_ORDER。
INSERT INTO pay_order (
  pay_order_no, order_no, user_id, pay_amount_cent, pay_status, pay_channel, transaction_no, callback_payload, notify_time, paid_at, idempotent_key, version, created_at, updated_at, deleted_at
) VALUES
('PAYPEXMATRIX202606020001', 'ORDPEXMATRIX202606020001', @matrix_user_id, 10100, 'SUCCESS', 'MOCK', 'MOCK-MATRIX-TXN-0001', '{"case":"paid_local_success"}', @now, @now, 'PAY:MOCK:ORDPEXMATRIX202606020001', 0, @now, @now, NULL),
('PAYPEXMATRIX202606020002', 'ORDPEXMATRIX202606020002', @matrix_user_id, 10200, 'SUCCESS', 'MOCK', 'MOCK-MATRIX-TXN-0002', '{"case":"paid_callback_processed"}', @now, @now, 'PAY:MOCK:ORDPEXMATRIX202606020002', 0, @now, @now, NULL),
('PAYPEXMATRIX202606020004', 'ORDPEXMATRIX202606020004', @matrix_user_id, 10400, 'CLOSED', 'MOCK', '', '{"case":"closed_unpaid"}', NULL, NULL, 'PAY:MOCK:ORDPEXMATRIX202606020004', 0, @now, @now, NULL),
('PAYPEXMATRIX202606020005', 'ORDPEXMATRIX202606020005', @matrix_user_id, 10500, 'FAILED', 'UNSUPPORTED_TEST_CHANNEL', '', '{"case":"local_failed_channel_unknown"}', NULL, NULL, 'PAY:UNSUPPORTED_TEST_CHANNEL:ORDPEXMATRIX202606020005', 0, @now, @now, NULL),
('PAYPEXMATRIX202606020006', 'ORDPEXMATRIX202606020006', @matrix_user_id, 9999, 'SUCCESS', 'MOCK', 'MOCK-MATRIX-TXN-0006', '{"case":"amount_mismatch"}', @now, @now, 'PAY:MOCK:ORDPEXMATRIX202606020006', 0, @now, @now, NULL),
('PAYPEXMATRIX202606020007', 'ORDPEXMATRIX202606020007', @matrix_user_id, 10700, 'SUCCESS', 'MOCK', 'MOCK-MATRIX-TXN-0007', '{"case":"verify_failed"}', @now, @now, 'PAY:MOCK:ORDPEXMATRIX202606020007', 0, @now, @now, NULL),
('PAYPEXMATRIX202606020008', 'ORDPEXMATRIX202606020008', @matrix_user_id, 10800, 'SUCCESS', 'MOCK', 'MOCK-MATRIX-TXN-0008', '{"case":"business_mismatch"}', @now, @now, 'PAY:MOCK:ORDPEXMATRIX202606020008', 0, @now, @now, NULL),
('PAYPEXMATRIX202606020009', 'ORDPEXMATRIX202606020009', @matrix_user_id, 10900, 'PAYING', 'MOCK', '', '{"case":"paying_unknown"}', NULL, NULL, 'PAY:MOCK:ORDPEXMATRIX202606020009', 0, @now, @now, NULL);

-- 7. 回调记录
-- 001 故意无回调：验证本地 SUCCESS + NO_CALLBACK 仍可 PAID_VERIFIED。
INSERT INTO pay_callback_record (
  channel, callback_type, pay_order_no, refund_no, order_no, out_trade_no, transaction_no, amount_cent, trade_status, signature, verified, process_status, fail_reason, raw_payload, callback_time, processed_at, created_at, updated_at
) VALUES
('MOCK', 'PAY', 'PAYPEXMATRIX202606020002', NULL, 'ORDPEXMATRIX202606020002', 'PAYPEXMATRIX202606020002', 'MOCK-MATRIX-TXN-0002', 10200, 'SUCCESS', 'mock-signature', 1, 'PROCESSED', '', '{"case":"callback_processed"}', @now, @now, @now, @now),
('MOCK', 'PAY', 'PAYPEXMATRIX202606020004', NULL, 'ORDPEXMATRIX202606020004', 'PAYPEXMATRIX202606020004', '', 10400, 'CLOSED', 'mock-signature', 1, 'IGNORED_NON_SUCCESS', '测试数据：支付关闭非成功回调', '{"case":"closed_unpaid"}', @now, @now, @now, @now),
('UNSUPPORTED_TEST_CHANNEL', 'PAY', 'PAYPEXMATRIX202606020005', NULL, 'ORDPEXMATRIX202606020005', 'PAYPEXMATRIX202606020005', '', 10500, 'FAILED', 'mock-signature', 1, 'IGNORED_NON_SUCCESS', '测试数据：支付失败非成功回调，渠道不可确认，用于 LOCAL_UNPAID/MEDIUM', '{"case":"failed_unpaid_channel_unknown"}', @now, @now, @now, @now),
('MOCK', 'PAY', 'PAYPEXMATRIX202606020006', NULL, 'ORDPEXMATRIX202606020006', 'PAYPEXMATRIX202606020006', 'MOCK-MATRIX-TXN-0006', 9999, 'SUCCESS', 'mock-signature', 1, 'AMOUNT_MISMATCH', '测试数据：支付金额与订单金额不一致', '{"case":"amount_mismatch"}', @now, @now, @now, @now),
('MOCK', 'PAY', 'PAYPEXMATRIX202606020007', NULL, 'ORDPEXMATRIX202606020007', 'PAYPEXMATRIX202606020007', 'MOCK-MATRIX-TXN-0007', 10700, 'SUCCESS', 'bad-signature', 0, 'VERIFY_FAILED', '测试数据：模拟回调验签失败', '{"case":"verify_failed"}', @now, @now, @now, @now),
('MOCK', 'PAY', 'PAYPEXMATRIX202606020008', NULL, 'ORDPEXMATRIX202606020008', 'PAYPEXMATRIX202606020008', 'MOCK-MATRIX-TXN-0008', 10800, 'SUCCESS', 'mock-signature', 1, 'BUSINESS_MISMATCH', '测试数据：模拟回调业务单号不匹配', '{"case":"business_mismatch"}', @now, @now, @now, @now),
('MOCK', 'PAY', 'PAYPEXMATRIX202606020009', NULL, 'ORDPEXMATRIX202606020009', 'PAYPEXMATRIX202606020009', '', 10900, 'PAYING', 'mock-signature', 1, 'RECEIVED', '测试数据：支付仍处理中，回调已接收未完成', '{"case":"paying_unknown"}', @now, NULL, @now, @now);

-- 8. 高风险/中风险对账记录，方便在对账管理查看闭环
INSERT INTO pay_reconciliation_record (
  biz_type, order_no, pay_order_no, refund_no, local_status, channel_status, local_amount_cent, channel_amount_cent, consistent, diff_type, repair_status, remark, repaired_at, created_at, updated_at
) VALUES
('PAY', 'ORDPEXMATRIX202606020005', 'PAYPEXMATRIX202606020005', NULL, 'PAYMENT_EXCEPTION/FAILED', 'CHANNEL_QUERY_FAILED', 10500, 10500, 1, 'LOCAL_UNPAID', 'NONE', '测试数据：本地支付失败但渠道不可确认，中风险未支付类场景', NULL, @now, @now),
('PAY', 'ORDPEXMATRIX202606020006', 'PAYPEXMATRIX202606020006', NULL, 'PAYMENT_EXCEPTION/SUCCESS', 'SUCCESS', 10600, 9999, 0, 'AMOUNT_MISMATCH', 'PENDING', '测试数据：订单金额与支付单金额不一致，需对账处理', NULL, @now, @now),
('PAY', 'ORDPEXMATRIX202606020007', 'PAYPEXMATRIX202606020007', NULL, 'PAYMENT_EXCEPTION/SUCCESS', 'SUCCESS', 10700, 10700, 0, 'CALLBACK_VERIFY_FAILED', 'PENDING', '测试数据：支付回调验签失败，需核查渠道签名', NULL, @now, @now),
('PAY', 'ORDPEXMATRIX202606020008', 'PAYPEXMATRIX202606020008', NULL, 'PAYMENT_EXCEPTION/SUCCESS', 'SUCCESS', 10800, 10800, 0, 'BUSINESS_MISMATCH', 'PENDING', '测试数据：支付回调业务身份不匹配，需对账处理', NULL, @now, @now),
('PAY', 'ORDPEXMATRIX202606020009', 'PAYPEXMATRIX202606020009', NULL, 'PAYMENT_EXCEPTION/PAYING', 'PAYING', 10900, 10900, 0, 'CHANNEL_UNKNOWN', 'PENDING', '测试数据：支付仍处理中，需稍后重试核验', NULL, @now, @now);

SET FOREIGN_KEY_CHECKS = 1;

-- 9. 执行后检查数据
SELECT
  o.order_no,
  o.order_status,
  o.pay_amount_cent AS order_amount_cent,
  p.pay_order_no,
  COALESCE(p.pay_status, 'NO_PAY_ORDER') AS pay_status,
  p.pay_amount_cent AS pay_amount_cent,
  COALESCE(c.process_status, 'NO_CALLBACK') AS callback_process_status,
  c.trade_status AS callback_trade_status,
  CASE o.order_no
    WHEN 'ORDPEXMATRIX202606020001' THEN 'LOW / PAID_VERIFIED / CONFIRM_PAID 可点'
    WHEN 'ORDPEXMATRIX202606020002' THEN 'LOW / PAID_VERIFIED / CONFIRM_PAID 可点'
    WHEN 'ORDPEXMATRIX202606020003' THEN 'LOW / NO_PAY_ORDER / 恢复待支付+关闭释放库存可点'
    WHEN 'ORDPEXMATRIX202606020004' THEN 'LOW / UNPAID_VERIFIED / 恢复待支付+关闭释放库存可点'
    WHEN 'ORDPEXMATRIX202606020005' THEN 'MEDIUM / LOCAL_UNPAID / 当前实现恢复待支付+关闭释放库存可点'
    WHEN 'ORDPEXMATRIX202606020006' THEN 'HIGH / AMOUNT_MISMATCH / 三按钮禁用'
    WHEN 'ORDPEXMATRIX202606020007' THEN 'HIGH / CALLBACK_VERIFY_FAILED / 三按钮禁用'
    WHEN 'ORDPEXMATRIX202606020008' THEN 'HIGH / BUSINESS_MISMATCH / 三按钮禁用'
    WHEN 'ORDPEXMATRIX202606020009' THEN 'HIGH / CHANNEL_UNKNOWN / 三按钮禁用'
  END AS expected_result
FROM oms_order o
LEFT JOIN pay_order p ON p.order_no = o.order_no
LEFT JOIN pay_callback_record c ON c.out_trade_no = p.pay_order_no
WHERE o.order_no LIKE 'ORDPEXMATRIX20260602%'
ORDER BY o.order_no;
