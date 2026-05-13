ALTER TABLE ims_stock_operation_log
    ADD COLUMN operator_type VARCHAR(32) NOT NULL DEFAULT '' COMMENT '操作人类型' AFTER remark,
    ADD COLUMN operator_id BIGINT NULL COMMENT '操作人ID' AFTER operator_type,
    ADD COLUMN operator_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '操作人名称' AFTER operator_id,
    ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT '' COMMENT '来源类型' AFTER operator_name;

CREATE INDEX idx_ims_stock_operation_log_source_type ON ims_stock_operation_log (source_type);
CREATE INDEX idx_ims_stock_operation_log_operator_id ON ims_stock_operation_log (operator_id);
