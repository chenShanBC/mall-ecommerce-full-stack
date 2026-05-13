-- 演示版数据清洗 SQL：精简类目、修正商品挂靠、去重商品
-- 适用对象：backend/sql/mall_fei.sql 对应的当前 mall_fei 数据
-- 执行前建议先备份数据库

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

START TRANSACTION;

-- 1) 仅保留 10 个一级类目为启用状态
UPDATE pms_category
SET status = 'ENABLED', deleted_at = NULL
WHERE id BETWEEN 1 AND 10;

-- 2) 其余二级测试类目全部禁用
UPDATE pms_category
SET status = 'DISABLED'
WHERE id > 10;

-- 3) 删除明显无意义的测试类目（如果你希望只禁用不删除，可注释此行）
UPDATE pms_category
SET deleted_at = NOW()
WHERE id > 10;

-- 4) 按商品名称语义，把商品统一挂到合理的一级类目
-- 类目映射约定：
-- 1 电脑办公：护眼台灯 / 机械键盘
-- 2 家用电器：空气炸锅
-- 3 个护清洁：防晒霜 / 电动牙刷
-- 4 食品生鲜：牛奶礼盒
-- 5 运动户外：跑步鞋 / 保温杯
-- 8 图书文创：无线耳机
-- 9 家居厨具：乳胶枕
-- 10 手机通讯：行李箱 / 双肩包

UPDATE pms_spu SET category_id = 5  WHERE name LIKE '%保温杯%' AND deleted_at IS NULL;
UPDATE pms_spu SET category_id = 5  WHERE name LIKE '%跑步鞋%' AND deleted_at IS NULL;
UPDATE pms_spu SET category_id = 1  WHERE name LIKE '%护眼台灯%' AND deleted_at IS NULL;
UPDATE pms_spu SET category_id = 2  WHERE name LIKE '%空气炸锅%' AND deleted_at IS NULL;
UPDATE pms_spu SET category_id = 8  WHERE name LIKE '%无线耳机%' AND deleted_at IS NULL;
UPDATE pms_spu SET category_id = 10 WHERE name LIKE '%行李箱%' AND deleted_at IS NULL;
UPDATE pms_spu SET category_id = 3  WHERE name LIKE '%防晒霜%' AND deleted_at IS NULL;
UPDATE pms_spu SET category_id = 1  WHERE name LIKE '%机械键盘%' AND deleted_at IS NULL;
UPDATE pms_spu SET category_id = 4  WHERE name LIKE '%牛奶礼盒%' AND deleted_at IS NULL;
UPDATE pms_spu SET category_id = 9  WHERE name LIKE '%乳胶枕%' AND deleted_at IS NULL;
UPDATE pms_spu SET category_id = 3  WHERE name LIKE '%电动牙刷%' AND deleted_at IS NULL;
UPDATE pms_spu SET category_id = 10 WHERE name LIKE '%双肩包%' AND deleted_at IS NULL;

-- 5) 对重复商品，仅保留每个名称 ID 最小的一条为在线，其余软删
UPDATE pms_spu s
JOIN (
    SELECT name, MIN(id) AS keep_id
    FROM pms_spu
    WHERE deleted_at IS NULL
    GROUP BY name
    HAVING COUNT(*) > 1
) k ON s.name = k.name
SET s.deleted_at = NOW(),
    s.status = 'OFFLINE'
WHERE s.id <> k.keep_id
  AND s.deleted_at IS NULL;

-- 6) 保留的商品统一设为 ONLINE，避免演示时部分主商品恰好离线
UPDATE pms_spu s
JOIN (
    SELECT MIN(id) AS keep_id
    FROM pms_spu
    WHERE deleted_at IS NULL
    GROUP BY name
) k ON s.id = k.keep_id
SET s.status = 'ONLINE';

-- 7) 同步清理被删除商品对应的 SKU：仅保留保留商品下的 SKU 在线，其余商品 SKU 软删
UPDATE pms_sku sku
JOIN pms_spu spu ON sku.spu_id = spu.id
SET sku.deleted_at = NOW(),
    sku.status = 'OFFLINE'
WHERE spu.deleted_at IS NOT NULL
  AND sku.deleted_at IS NULL;

UPDATE pms_sku sku
JOIN pms_spu spu ON sku.spu_id = spu.id
SET sku.status = 'ONLINE',
    sku.deleted_at = NULL
WHERE spu.deleted_at IS NULL;

-- 8) 清理已删除商品残留在购物车中的数据，避免结算脏数据
UPDATE cart_item ci
JOIN pms_sku sku ON ci.sku_id = sku.id
JOIN pms_spu spu ON sku.spu_id = spu.id
SET ci.deleted_at = NOW()
WHERE spu.deleted_at IS NOT NULL
  AND ci.deleted_at IS NULL;

COMMIT;

SET FOREIGN_KEY_CHECKS = 1;

-- 清洗后建议执行以下检查：
-- 1) SELECT COUNT(*) FROM pms_category WHERE deleted_at IS NULL;
-- 2) SELECT id,name,parent_id,status FROM pms_category WHERE deleted_at IS NULL ORDER BY id;
-- 3) SELECT id,name,category_id,status FROM pms_spu WHERE deleted_at IS NULL ORDER BY id;
-- 4) SELECT name,COUNT(*) FROM pms_spu WHERE deleted_at IS NULL GROUP BY name HAVING COUNT(*) > 1;
