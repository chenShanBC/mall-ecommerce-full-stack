-- 一期订单支付异常闭环：以订单受控中间态承接支付异常，暂停超时关单/履约，支持人工确认或关闭释放库存。
-- 可重复执行：兼容不支持 DROP INDEX IF EXISTS / ADD INDEX IF NOT EXISTS 的 MySQL 客户端或版本。

SET @index_name := 'idx_oms_order_status_expire_order_no';
SET @drop_sql := (
    SELECT IF(
        COUNT(1) > 0,
        CONCAT('ALTER TABLE oms_order DROP INDEX ', @index_name),
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'oms_order'
      AND index_name = @index_name
);
PREPARE drop_stmt FROM @drop_sql;
EXECUTE drop_stmt;
DEALLOCATE PREPARE drop_stmt;

ALTER TABLE oms_order
    ADD INDEX idx_oms_order_status_expire_order_no (order_status, expire_time, order_no);

-- 如果生产环境对订单状态有 CHECK/枚举约束，请将 PAYMENT_EXCEPTION 加入允许值。
-- 本项目当前 oms_order.order_status 为 varchar，无额外 CHECK 约束。
