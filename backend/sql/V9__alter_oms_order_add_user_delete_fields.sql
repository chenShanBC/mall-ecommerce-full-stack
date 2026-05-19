ALTER TABLE oms_order
    ADD COLUMN user_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '用户侧是否删除：0-否，1-是' AFTER completed_at,
    ADD COLUMN user_deleted_at DATETIME NULL COMMENT '用户侧删除时间' AFTER user_deleted;

CREATE INDEX idx_oms_order_user_deleted ON oms_order (user_id, user_deleted, id);
