CREATE TABLE IF NOT EXISTS ims_stock_reconciliation_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    status VARCHAR(32) NOT NULL COMMENT '对账状态：CONSISTENT/INCONSISTENT/REPAIRED/IGNORED',
    severity VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '差异等级',
    stock_snapshot_json JSON NULL COMMENT '库存表快照',
    expected_snapshot_json JSON NULL COMMENT 'DB锁记录计算快照',
    redis_snapshot_json JSON NULL COMMENT 'Redis快照',
    differences_json JSON NULL COMMENT '差异明细',
    repair_status VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '修复状态：NONE/PENDING/DONE/IGNORED',
    repair_remark VARCHAR(512) NULL COMMENT '修复或忽略说明',
    checked_at DATETIME NOT NULL COMMENT '校验时间',
    repaired_at DATETIME NULL COMMENT '处理时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_stock_reconciliation_sku_checked (sku_id, checked_at),
    KEY idx_stock_reconciliation_status_checked (status, checked_at),
    KEY idx_stock_reconciliation_repair_status (repair_status)
) COMMENT='库存对账记录表';
