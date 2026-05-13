ALTER TABLE ums_admin
    ADD COLUMN user_id BIGINT NULL COMMENT '关联用户ID' AFTER id,
    ADD COLUMN permissions_json JSON NULL COMMENT '权限集合JSON' AFTER role_code;

UPDATE ums_admin
SET permissions_json = JSON_ARRAY(
    'system:account:manage',
    'system:log:view',
    'product:view',
    'product:manage',
    'order:view',
    'order:manage',
    'stock:view',
    'stock:manage'
)
WHERE role_code = 'SUPER_ADMIN' AND (permissions_json IS NULL OR JSON_LENGTH(permissions_json) = 0);

CREATE TABLE ums_admin_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    operator_admin_id BIGINT NOT NULL COMMENT '操作管理员ID',
    operator_username VARCHAR(50) NOT NULL COMMENT '操作管理员账号',
    operation_module VARCHAR(50) NOT NULL COMMENT '操作模块',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    operation_content VARCHAR(500) NOT NULL COMMENT '操作内容',
    operation_result VARCHAR(50) NOT NULL DEFAULT 'SUCCESS' COMMENT '操作结果',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_ums_admin_operation_log_admin_id (operator_admin_id),
    KEY idx_ums_admin_operation_log_module (operation_module)
) COMMENT='运营后台操作日志表';
