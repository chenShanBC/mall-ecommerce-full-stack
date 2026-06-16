-- 库存流水历史重复数据清理与唯一索引创建脚本
-- 适用场景：执行 stock_production_enhancement_migration.sql 时，创建 uk_ims_stock_operation_log_biz 失败，提示 Duplicate entry。
-- 处理策略：
-- 1. 先备份所有重复业务键对应的历史流水；
-- 2. 对同一 business_type + business_no + sku_id + operation_type，只保留 id 最小的一条；
-- 3. 再创建业务幂等唯一索引。

USE `mall_fei`;

-- =========================================================
-- 1. 查看当前重复流水
-- =========================================================

SELECT `business_type`, `business_no`, `sku_id`, `operation_type`, COUNT(*) AS cnt
FROM `ims_stock_operation_log`
GROUP BY `business_type`, `business_no`, `sku_id`, `operation_type`
HAVING cnt > 1;

-- =========================================================
-- 2. 备份重复流水明细
-- =========================================================

CREATE TABLE IF NOT EXISTS `bak_ims_stock_operation_log_duplicate_20260524` AS
SELECT l.*
FROM `ims_stock_operation_log` l
JOIN (
    SELECT `business_type`, `business_no`, `sku_id`, `operation_type`
    FROM `ims_stock_operation_log`
    GROUP BY `business_type`, `business_no`, `sku_id`, `operation_type`
    HAVING COUNT(*) > 1
) d ON l.`business_type` = d.`business_type`
   AND l.`business_no` = d.`business_no`
   AND l.`sku_id` = d.`sku_id`
   AND l.`operation_type` = d.`operation_type`;

-- =========================================================
-- 3. 删除重复流水，只保留每组 id 最小的一条
-- =========================================================

DELETE l
FROM `ims_stock_operation_log` l
JOIN (
    SELECT `business_type`, `business_no`, `sku_id`, `operation_type`, MIN(`id`) AS keep_id
    FROM `ims_stock_operation_log`
    GROUP BY `business_type`, `business_no`, `sku_id`, `operation_type`
    HAVING COUNT(*) > 1
) d ON l.`business_type` = d.`business_type`
   AND l.`business_no` = d.`business_no`
   AND l.`sku_id` = d.`sku_id`
   AND l.`operation_type` = d.`operation_type`
   AND l.`id` <> d.`keep_id`;

-- =========================================================
-- 4. 再次检查重复流水，应无结果
-- =========================================================

SELECT `business_type`, `business_no`, `sku_id`, `operation_type`, COUNT(*) AS cnt
FROM `ims_stock_operation_log`
GROUP BY `business_type`, `business_no`, `sku_id`, `operation_type`
HAVING cnt > 1;

-- =========================================================
-- 5. 创建业务幂等唯一索引
-- =========================================================

ALTER TABLE `ims_stock_operation_log`
    ADD UNIQUE INDEX `uk_ims_stock_operation_log_biz`(`business_type`, `business_no`, `sku_id`, `operation_type`);

-- =========================================================
-- 6. 验证索引是否创建成功
-- =========================================================

SHOW INDEX FROM `ims_stock_operation_log` WHERE Key_name = 'uk_ims_stock_operation_log_biz';
