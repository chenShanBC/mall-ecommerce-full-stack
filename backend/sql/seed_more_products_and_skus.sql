-- 补数 SQL：新增商品 + 扩充原有商品 SKU 规格
-- 使用前提：当前库已完成 cleanup_mall_fei_demo.sql 清洗
-- 目标：
-- 1) 新增一批不重复、类目正确的新商品
-- 2) 给现有保留商品补更多 SKU 规格
-- 3) 自动补充库存

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

START TRANSACTION;

-- =========================================================
-- A. 新增一批全新商品（名称不与现有重复）
-- 一级类目约定：
-- 1 电脑办公
-- 2 家用电器
-- 3 个护清洁
-- 4 食品生鲜
-- 5 运动户外
-- 6 服饰内衣
-- 7 母婴用品
-- 8 图书文创
-- 9 家居厨具
-- 10 手机通讯
-- =========================================================

INSERT INTO pms_spu (name, category_id, main_image_url, album_images_json, description, status, version, created_at, updated_at, deleted_at)
VALUES
('曜川 4K显示器 轻享版', 1, 'https://img.mallfei.local/product/spu/new-monitor.jpg', '["https://img.mallfei.local/product/spu/new-monitor-1.jpg"]', '高分辨率显示器，适合办公与轻度设计。', 'ONLINE', 0, NOW(), NOW(), NULL),
('墨岚 人体工学椅 Pro', 1, 'https://img.mallfei.local/product/spu/new-chair.jpg', '["https://img.mallfei.local/product/spu/new-chair-1.jpg"]', '久坐舒适的人体工学座椅，适合家庭办公。', 'ONLINE', 0, NOW(), NOW(), NULL),
('清屿 迷你烤箱 标准款', 2, 'https://img.mallfei.local/product/spu/new-oven.jpg', '["https://img.mallfei.local/product/spu/new-oven-1.jpg"]', '适合单人家庭使用的迷你烤箱。', 'ONLINE', 0, NOW(), NOW(), NULL),
('沐序 加湿器 静音版', 2, 'https://img.mallfei.local/product/spu/new-humidifier.jpg', '["https://img.mallfei.local/product/spu/new-humidifier-1.jpg"]', '静音轻雾设计，适合卧室与书房。', 'ONLINE', 0, NOW(), NOW(), NULL),
('初芽 洁面仪 温润版', 3, 'https://img.mallfei.local/product/spu/new-facewash.jpg', '["https://img.mallfei.local/product/spu/new-facewash-1.jpg"]', '温和清洁护理，适合日常护肤。', 'ONLINE', 0, NOW(), NOW(), NULL),
('若汐 香氛洗手液 礼盒装', 3, 'https://img.mallfei.local/product/spu/new-handwash.jpg', '["https://img.mallfei.local/product/spu/new-handwash-1.jpg"]', '清新香氛搭配细腻泡沫，适合家庭场景。', 'ONLINE', 0, NOW(), NOW(), NULL),
('禾川 坚果礼盒 甄选装', 4, 'https://img.mallfei.local/product/spu/new-nuts.jpg', '["https://img.mallfei.local/product/spu/new-nuts-1.jpg"]', '多种坚果搭配，适合送礼与自食。', 'ONLINE', 0, NOW(), NOW(), NULL),
('森野 冷萃咖啡 体验装', 4, 'https://img.mallfei.local/product/spu/new-coffee.jpg', '["https://img.mallfei.local/product/spu/new-coffee-1.jpg"]', '轻度烘焙风味，适合夏日冷饮。', 'ONLINE', 0, NOW(), NOW(), NULL),
('拓野 登山包 进阶款', 5, 'https://img.mallfei.local/product/spu/new-hikingbag.jpg', '["https://img.mallfei.local/product/spu/new-hikingbag-1.jpg"]', '适合徒步与轻露营使用的功能背包。', 'ONLINE', 0, NOW(), NOW(), NULL),
('凌跃 运动短袖 速干版', 5, 'https://img.mallfei.local/product/spu/new-sportshirt.jpg', '["https://img.mallfei.local/product/spu/new-sportshirt-1.jpg"]', '透气速干面料，适合跑步训练。', 'ONLINE', 0, NOW(), NOW(), NULL),
('拾棉 家居睡衣 套装', 6, 'https://img.mallfei.local/product/spu/new-pajama.jpg', '["https://img.mallfei.local/product/spu/new-pajama-1.jpg"]', '亲肤柔软，适合居家穿着。', 'ONLINE', 0, NOW(), NOW(), NULL),
('青禾 牛仔外套 轻复古款', 6, 'https://img.mallfei.local/product/spu/new-jacket.jpg', '["https://img.mallfei.local/product/spu/new-jacket-1.jpg"]', '简洁百搭，适合春秋通勤。', 'ONLINE', 0, NOW(), NOW(), NULL),
('芽芽 婴儿餐椅 安心版', 7, 'https://img.mallfei.local/product/spu/new-babychair.jpg', '["https://img.mallfei.local/product/spu/new-babychair-1.jpg"]', '稳固安全，适合婴幼儿进食场景。', 'ONLINE', 0, NOW(), NOW(), NULL),
('启芽 儿童积木 创意盒', 7, 'https://img.mallfei.local/product/spu/new-blocks.jpg', '["https://img.mallfei.local/product/spu/new-blocks-1.jpg"]', '启蒙拼搭，适合亲子陪伴。', 'ONLINE', 0, NOW(), NOW(), NULL),
('知白 钢笔礼盒 商务款', 8, 'https://img.mallfei.local/product/spu/new-pen.jpg', '["https://img.mallfei.local/product/spu/new-pen-1.jpg"]', '书写顺滑，适合办公送礼。', 'ONLINE', 0, NOW(), NOW(), NULL),
('木言 手账套装 灵感版', 8, 'https://img.mallfei.local/product/spu/new-notebook.jpg', '["https://img.mallfei.local/product/spu/new-notebook-1.jpg"]', '适合记录与手账创作。', 'ONLINE', 0, NOW(), NOW(), NULL),
('清木 原木餐盘 家庭装', 9, 'https://img.mallfei.local/product/spu/new-plate.jpg', '["https://img.mallfei.local/product/spu/new-plate-1.jpg"]', '简约原木风格，适合家用餐桌。', 'ONLINE', 0, NOW(), NOW(), NULL),
('泊云 香薰夜灯 暖光款', 9, 'https://img.mallfei.local/product/spu/new-nightlamp.jpg', '["https://img.mallfei.local/product/spu/new-nightlamp-1.jpg"]', '香薰与照明结合，营造舒缓氛围。', 'ONLINE', 0, NOW(), NOW(), NULL),
('迅界 充电宝 轻量版', 10, 'https://img.mallfei.local/product/spu/new-powerbank.jpg', '["https://img.mallfei.local/product/spu/new-powerbank-1.jpg"]', '轻巧便携，适合日常通勤补电。', 'ONLINE', 0, NOW(), NOW(), NULL),
('曜讯 蓝牙音箱 户外版', 10, 'https://img.mallfei.local/product/spu/new-speaker.jpg', '["https://img.mallfei.local/product/spu/new-speaker-1.jpg"]', '便携蓝牙连接，适合户外聚会。', 'ONLINE', 0, NOW(), NOW(), NULL);

-- =========================================================
-- B. 为新增商品生成 2~3 个 SKU
-- 规则：取刚插入的新商品，根据名称匹配插入 SKU
-- =========================================================

INSERT INTO pms_sku (spu_id, sku_code, sku_name, spec_json, sale_price_cent, origin_price_cent, sales_count, status, version, created_at, updated_at, deleted_at)
SELECT s.id,
       CONCAT('MFN', LPAD(s.id, 6, '0'), '01'),
       CONCAT(s.name, ' 默认款'),
       '{"规格":"默认款","颜色":"标准色"}',
       89900, 99900, 0, 'ONLINE', 0, NOW(), NOW(), NULL
FROM pms_spu s
WHERE s.name IN (
'曜川 4K显示器 轻享版','墨岚 人体工学椅 Pro','清屿 迷你烤箱 标准款','沐序 加湿器 静音版','初芽 洁面仪 温润版',
'若汐 香氛洗手液 礼盒装','禾川 坚果礼盒 甄选装','森野 冷萃咖啡 体验装','拓野 登山包 进阶款','凌跃 运动短袖 速干版',
'拾棉 家居睡衣 套装','青禾 牛仔外套 轻复古款','芽芽 婴儿餐椅 安心版','启芽 儿童积木 创意盒','知白 钢笔礼盒 商务款',
'木言 手账套装 灵感版','清木 原木餐盘 家庭装','泊云 香薰夜灯 暖光款','迅界 充电宝 轻量版','曜讯 蓝牙音箱 户外版'
)
AND NOT EXISTS (
    SELECT 1 FROM pms_sku sku WHERE sku.spu_id = s.id
);

INSERT INTO pms_sku (spu_id, sku_code, sku_name, spec_json, sale_price_cent, origin_price_cent, sales_count, status, version, created_at, updated_at, deleted_at)
SELECT s.id,
       CONCAT('MFN', LPAD(s.id, 6, '0'), '02'),
       CONCAT(s.name, ' 升级款'),
       '{"规格":"升级款","颜色":"高级灰"}',
       109900, 119900, 0, 'ONLINE', 0, NOW(), NOW(), NULL
FROM pms_spu s
WHERE s.name IN (
'曜川 4K显示器 轻享版','墨岚 人体工学椅 Pro','清屿 迷你烤箱 标准款','沐序 加湿器 静音版','初芽 洁面仪 温润版',
'若汐 香氛洗手液 礼盒装','禾川 坚果礼盒 甄选装','森野 冷萃咖啡 体验装','拓野 登山包 进阶款','凌跃 运动短袖 速干版',
'拾棉 家居睡衣 套装','青禾 牛仔外套 轻复古款','芽芽 婴儿餐椅 安心版','启芽 儿童积木 创意盒','知白 钢笔礼盒 商务款',
'木言 手账套装 灵感版','清木 原木餐盘 家庭装','泊云 香薰夜灯 暖光款','迅界 充电宝 轻量版','曜讯 蓝牙音箱 户外版'
)
AND NOT EXISTS (
    SELECT 1 FROM pms_sku sku WHERE sku.spu_id = s.id AND sku.sku_code = CONCAT('MFN', LPAD(s.id, 6, '0'), '02')
);

INSERT INTO pms_sku (spu_id, sku_code, sku_name, spec_json, sale_price_cent, origin_price_cent, sales_count, status, version, created_at, updated_at, deleted_at)
SELECT s.id,
       CONCAT('MFN', LPAD(s.id, 6, '0'), '03'),
       CONCAT(s.name, ' 旗舰款'),
       '{"规格":"旗舰款","颜色":"曜石黑"}',
       129900, 139900, 0, 'ONLINE', 0, NOW(), NOW(), NULL
FROM pms_spu s
WHERE s.name IN ('曜川 4K显示器 轻享版','墨岚 人体工学椅 Pro','拓野 登山包 进阶款','芽芽 婴儿餐椅 安心版','迅界 充电宝 轻量版','曜讯 蓝牙音箱 户外版')
AND NOT EXISTS (
    SELECT 1 FROM pms_sku sku WHERE sku.spu_id = s.id AND sku.sku_code = CONCAT('MFN', LPAD(s.id, 6, '0'), '03')
);

-- =========================================================
-- C. 给现有保留商品补更多 SKU（不同规格）
-- 仅对当前仍保留的主商品执行，不重复插入相同 sku_code
-- =========================================================

INSERT INTO pms_sku (spu_id, sku_code, sku_name, spec_json, sale_price_cent, origin_price_cent, sales_count, status, version, created_at, updated_at, deleted_at)
SELECT s.id,
       CONCAT('MFX', LPAD(s.id, 6, '0'), 'A1'),
       CONCAT(s.name, ' 进阶版'),
       '{"规格":"进阶版","颜色":"月岩灰"}',
       GREATEST(ROUND(base.sale_price_cent * 1.12), base.sale_price_cent + 2000),
       GREATEST(ROUND(base.origin_price_cent * 1.12), base.origin_price_cent + 2500),
       0, 'ONLINE', 0, NOW(), NOW(), NULL
FROM pms_spu s
JOIN (
    SELECT spu_id, MIN(sale_price_cent) AS sale_price_cent, MIN(origin_price_cent) AS origin_price_cent
    FROM pms_sku
    WHERE deleted_at IS NULL
    GROUP BY spu_id
) base ON base.spu_id = s.id
WHERE s.deleted_at IS NULL
  AND s.status = 'ONLINE'
  AND NOT EXISTS (
      SELECT 1 FROM pms_sku sku WHERE sku.spu_id = s.id AND sku.sku_code = CONCAT('MFX', LPAD(s.id, 6, '0'), 'A1')
  );

INSERT INTO pms_sku (spu_id, sku_code, sku_name, spec_json, sale_price_cent, origin_price_cent, sales_count, status, version, created_at, updated_at, deleted_at)
SELECT s.id,
       CONCAT('MFX', LPAD(s.id, 6, '0'), 'B1'),
       CONCAT(s.name, ' 轻享版2'),
       '{"规格":"轻享版2","颜色":"云雾白"}',
       GREATEST(ROUND(base.sale_price_cent * 0.94), 100),
       GREATEST(ROUND(base.origin_price_cent * 0.96), 200),
       0, 'ONLINE', 0, NOW(), NOW(), NULL
FROM pms_spu s
JOIN (
    SELECT spu_id, MIN(sale_price_cent) AS sale_price_cent, MIN(origin_price_cent) AS origin_price_cent
    FROM pms_sku
    WHERE deleted_at IS NULL
    GROUP BY spu_id
) base ON base.spu_id = s.id
WHERE s.deleted_at IS NULL
  AND s.status = 'ONLINE'
  AND NOT EXISTS (
      SELECT 1 FROM pms_sku sku WHERE sku.spu_id = s.id AND sku.sku_code = CONCAT('MFX', LPAD(s.id, 6, '0'), 'B1')
  );

-- =========================================================
-- D. 给所有新增 SKU 自动补库存
-- 如果 stock 不存在则新增，避免重复插入
-- =========================================================

INSERT INTO ims_stock (sku_id, total_stock, locked_stock, available_stock, version, created_at, updated_at)
SELECT sku.id, 180, 0, 180, 0, NOW(), NOW()
FROM pms_sku sku
LEFT JOIN ims_stock st ON st.sku_id = sku.id
WHERE st.id IS NULL
  AND sku.deleted_at IS NULL;

COMMIT;

SET FOREIGN_KEY_CHECKS = 1;

-- 建议执行后检查：
-- 1) SELECT COUNT(*) FROM pms_spu WHERE deleted_at IS NULL;
-- 2) SELECT COUNT(*) FROM pms_sku WHERE deleted_at IS NULL;
-- 3) SELECT s.id, s.name, s.category_id, COUNT(k.id) sku_num FROM pms_spu s LEFT JOIN pms_sku k ON k.spu_id = s.id AND k.deleted_at IS NULL WHERE s.deleted_at IS NULL GROUP BY s.id, s.name, s.category_id ORDER BY s.id DESC;
