USE mall_fei;

-- 修复当前库中二级类目 parent_id 与名称前缀错位的问题
-- 规则：根据二级类目名称前缀，回填到正确的一级类目 id
UPDATE pms_category child
JOIN pms_category parent
  ON parent.parent_id = 0
 AND parent.level = 1
 AND child.level = 2
 AND child.parent_id > 0
 AND child.name LIKE CONCAT(parent.name, '-%')
SET child.parent_id = parent.id
WHERE child.parent_id <> parent.id;

-- 修复二级类目名称后缀固定绑定的问题
-- 目标分布：
-- 11-20  -> 热卖
-- 21-30  -> 新品
-- 31-40  -> 精选
-- 41-50  -> 礼赠
-- 51-60  -> 清仓
-- 61-70  -> 热卖
-- ... 依此循环
UPDATE pms_category child
JOIN pms_category parent ON parent.id = child.parent_id
SET child.name = CONCAT(
    parent.name,
    '-',
    ELT(MOD(FLOOR((child.id - 11) / 10), 5) + 1, '热卖', '新品', '精选', '礼赠', '清仓')
)
WHERE child.level = 2
  AND child.parent_id > 0;

-- 校验查询
SELECT child.id,
       child.name,
       child.parent_id,
       parent.name AS parent_name
FROM pms_category child
LEFT JOIN pms_category parent ON parent.id = child.parent_id
WHERE child.level = 2
ORDER BY child.id;
