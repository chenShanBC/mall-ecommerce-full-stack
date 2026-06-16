-- 后台运营账号权限刷新迁移脚本
-- 目的：按最新内置角色默认权限刷新 ums_admin.permissions_json，补齐新增功能权限。
-- 说明：permissions_json 是账号实际生效权限快照；本脚本适合开发/测试环境或确认需要按角色重置权限的账号批量执行。
-- 风险：如果某些账号做过个性化权限调整，执行后会被角色默认权限覆盖。生产执行前请先备份并确认影响范围。

START TRANSACTION;

-- 执行前确认影响范围
SELECT id, username, nickname, role_code, permissions_json
FROM ums_admin
WHERE deleted_at IS NULL
ORDER BY id;

-- 超级管理员：补齐全部后台权限
UPDATE ums_admin
SET permissions_json = JSON_ARRAY(
        'dashboard:view',
        'admin:view', 'admin:create', 'admin:update', 'admin:disable',
        'role:view', 'role:manage',
        'permission:view', 'permission:assign',
        'log:operation:view',
        'user:view', 'user:edit', 'user:disable',
        'product:view', 'product:create', 'product:update', 'product:on_sale', 'product:off_sale', 'category:manage',
        'stock:view', 'stock:log:view', 'stock:adjust', 'stock:reconcile:view', 'stock:reconcile:check', 'stock:reconcile:repair',
        'order:view', 'order:remark', 'order:ship', 'order:close', 'order:log:view',
        'aftersale:view', 'aftersale:audit',
        'refund:view', 'refund:execute',
        'finance:view', 'payment:view', 'reconciliation:view', 'reconciliation:handle'
    ),
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND UPPER(role_code) = 'SUPER_ADMIN';

-- 商品运营：商品维护 + 库存只读/库存流水
UPDATE ums_admin
SET permissions_json = JSON_ARRAY(
        'dashboard:view',
        'product:view', 'product:create', 'product:update', 'product:on_sale', 'product:off_sale', 'category:manage',
        'stock:view', 'stock:log:view'
    ),
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND UPPER(role_code) = 'PRODUCT_OPERATOR';

-- 订单运营：订单处理 + 售后审核
UPDATE ums_admin
SET permissions_json = JSON_ARRAY(
        'dashboard:view',
        'order:view', 'order:remark', 'order:ship', 'order:close', 'order:log:view',
        'aftersale:view', 'aftersale:audit',
        'refund:view'
    ),
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND UPPER(role_code) = 'ORDER_OPERATOR';

-- 库存运营：库存调整 + 库存对账查看/校验/修复
UPDATE ums_admin
SET permissions_json = JSON_ARRAY(
        'dashboard:view',
        'stock:view', 'stock:log:view', 'stock:adjust', 'stock:reconcile:view', 'stock:reconcile:check', 'stock:reconcile:repair',
        'product:view'
    ),
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND UPPER(role_code) = 'STOCK_OPERATOR';

-- 财务运营：支付、退款、支付对账；库存对账归库存运营处理
UPDATE ums_admin
SET permissions_json = JSON_ARRAY(
        'dashboard:view',
        'finance:view', 'payment:view',
        'refund:view', 'refund:execute',
        'reconciliation:view', 'reconciliation:handle',
        'order:view', 'aftersale:view'
    ),
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND UPPER(role_code) = 'FINANCE_OPERATOR';

-- 客服：用户查看 + 订单备注 + 售后查看
UPDATE ums_admin
SET permissions_json = JSON_ARRAY(
        'dashboard:view',
        'user:view',
        'order:view', 'order:remark', 'order:log:view',
        'aftersale:view'
    ),
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND UPPER(role_code) = 'CUSTOMER_SERVICE';

-- 用户运营：用户资料维护
UPDATE ums_admin
SET permissions_json = JSON_ARRAY(
        'dashboard:view',
        'user:view', 'user:edit', 'user:disable',
        'order:view',
        'log:operation:view'
    ),
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND UPPER(role_code) = 'USER_OPERATOR';

-- 审计员：审计只读 + 对账查看 + 库存一致性校验；不授予支付单管理、退款执行、对账处理、库存修复等写权限
UPDATE ums_admin
SET permissions_json = JSON_ARRAY(
        'dashboard:view',
        'user:view',
        'product:view',
        'stock:view', 'stock:log:view', 'stock:reconcile:view', 'stock:reconcile:check',
        'order:view', 'order:log:view',
        'aftersale:view',
        'refund:view',
        'finance:view', 'reconciliation:view',
        'admin:view', 'role:view', 'permission:view', 'log:operation:view'
    ),
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND UPPER(role_code) = 'AUDIT_OPERATOR';

-- 执行后核验
SELECT role_code, COUNT(*) AS account_count
FROM ums_admin
WHERE deleted_at IS NULL
GROUP BY role_code
ORDER BY role_code;

SELECT id, username, nickname, role_code, permissions_json
FROM ums_admin
WHERE deleted_at IS NULL
ORDER BY id;

COMMIT;
