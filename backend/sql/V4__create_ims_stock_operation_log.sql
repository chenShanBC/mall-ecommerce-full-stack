CREATE TABLE ims_stock_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库存操作日志ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    operation_type VARCHAR(32) NOT NULL COMMENT '操作类型',
    business_type VARCHAR(32) NOT NULL DEFAULT '' COMMENT '业务类型',
    business_no VARCHAR(64) NOT NULL DEFAULT '' COMMENT '业务单号',
    change_quantity INT NOT NULL DEFAULT 0 COMMENT '变更数量',
    before_total_stock INT NOT NULL DEFAULT 0 COMMENT '变更前总库存',
    before_locked_stock INT NOT NULL DEFAULT 0 COMMENT '变更前锁定库存',
    before_available_stock INT NOT NULL DEFAULT 0 COMMENT '变更前可用库存',
    after_total_stock INT NOT NULL DEFAULT 0 COMMENT '变更后总库存',
    after_locked_stock INT NOT NULL DEFAULT 0 COMMENT '变更后锁定库存',
    after_available_stock INT NOT NULL DEFAULT 0 COMMENT '变更后可用库存',
    remark VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_ims_stock_operation_log_sku_id (sku_id),
    KEY idx_ims_stock_operation_log_operation_type (operation_type),
    KEY idx_ims_stock_operation_log_created_at (created_at)
) COMMENT='库存操作日志表';
