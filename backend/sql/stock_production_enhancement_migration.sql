-- 库存模块生产级增强表结构变更脚本
-- 适用场景：在已有 mall_fei 数据库上增量执行
-- 变更内容：
-- 1. ims_stock_lock 增加 MQ/DB 同步幂等标记字段
-- 2. ims_stock_lock 增加未同步预占扫描索引
-- 3. ims_stock_operation_log 增加业务幂等唯一索引

USE `mall_fei`;

-- =========================================================
-- 1. 库存锁表：增加 DB 同步幂等字段
-- =========================================================

ALTER TABLE `ims_stock_lock`
    ADD COLUMN `reserved_synced` tinyint(1) NOT NULL DEFAULT 0 COMMENT '预占库存是否已同步DB库存表' AFTER `deduct_time`,
    ADD COLUMN `cancelled_synced` tinyint(1) NOT NULL DEFAULT 0 COMMENT '取消预占是否已同步DB库存表' AFTER `reserved_synced`,
    ADD COLUMN `confirmed_synced` tinyint(1) NOT NULL DEFAULT 0 COMMENT '确认扣减是否已同步DB库存表' AFTER `cancelled_synced`;

-- 历史数据兼容：
-- 既有 RESERVED / CANCELLED / CONFIRMED 锁记录大概率已经反映在 ims_stock 库存表中，
-- 因此统一标记为已同步，避免上线后补偿任务或 MQ 重放导致历史记录重复同步。
UPDATE `ims_stock_lock`
SET `reserved_synced` = 1
WHERE `status` IN ('RESERVED', 'CANCELLED', 'CONFIRMED');

UPDATE `ims_stock_lock`
SET `cancelled_synced` = 1
WHERE `status` = 'CANCELLED';

UPDATE `ims_stock_lock`
SET `confirmed_synced` = 1
WHERE `status` = 'CONFIRMED';

-- 未完成预占同步扫描索引：用于补偿任务快速定位 RESERVED 且 reserved_synced=0 的记录。
ALTER TABLE `ims_stock_lock`
    ADD INDEX `idx_ims_stock_lock_sync_status`(`status`, `reserved_synced`, `lock_time`);

-- =========================================================
-- 2. 库存流水表：增加业务幂等唯一索引
-- =========================================================

-- 注意：如果历史数据中存在重复流水，下面的唯一索引会创建失败。
-- 可先执行下方重复数据检查 SQL。
--
-- SELECT `business_type`, `business_no`, `sku_id`, `operation_type`, COUNT(*) AS cnt
-- FROM `ims_stock_operation_log`
-- GROUP BY `business_type`, `business_no`, `sku_id`, `operation_type`
-- HAVING cnt > 1;

ALTER TABLE `ims_stock_operation_log`
    ADD UNIQUE INDEX `uk_ims_stock_operation_log_biz`(`business_type`, `business_no`, `sku_id`, `operation_type`);

-- =========================================================
-- 3. 变更后检查 SQL
-- =========================================================

-- 检查库存锁同步字段
SELECT `id`, `sku_id`, `business_type`, `business_no`, `status`,
       `reserved_synced`, `cancelled_synced`, `confirmed_synced`
FROM `ims_stock_lock`
ORDER BY `id` DESC
LIMIT 20;

-- 检查库存表数量约束是否正常
SELECT `sku_id`, `total_stock`, `locked_stock`, `available_stock`
FROM `ims_stock`
WHERE `total_stock` <> `locked_stock` + `available_stock`;

-- 检查库存流水业务幂等重复数据
SELECT `business_type`, `business_no`, `sku_id`, `operation_type`, COUNT(*) AS cnt
FROM `ims_stock_operation_log`
GROUP BY `business_type`, `business_no`, `sku_id`, `operation_type`
HAVING cnt > 1;
