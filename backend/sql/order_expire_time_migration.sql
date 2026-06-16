-- 订单支付过期时间生产级增强迁移脚本
-- 适用场景：在已有 mall_fei 数据库上增量执行
-- 变更内容：
-- 1. oms_order 增加 expire_time，作为订单支付过期的唯一事实来源
-- 2. 使用当前配置的 15 分钟为历史订单回填 expire_time
-- 3. 增加待支付超时扫描索引

USE `mall_fei`;

ALTER TABLE `oms_order`
    ADD COLUMN `expire_time` datetime NULL COMMENT '订单支付过期时间' AFTER `completed_at`;

UPDATE `oms_order`
SET `expire_time` = DATE_ADD(`created_at`, INTERVAL 15 MINUTE)
WHERE `expire_time` IS NULL;

ALTER TABLE `oms_order`
    MODIFY COLUMN `expire_time` datetime NOT NULL COMMENT '订单支付过期时间' AFTER `completed_at`,
    ADD INDEX `idx_oms_order_status_expire_time`(`order_status`, `expire_time`);

-- 变更后检查
SELECT `id`, `order_no`, `order_status`, `created_at`, `expire_time`
FROM `oms_order`
ORDER BY `id` DESC
LIMIT 20;
