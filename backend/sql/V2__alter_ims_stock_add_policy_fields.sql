ALTER TABLE ims_stock
    ADD COLUMN stock_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '库存状态' AFTER available_stock,
    ADD COLUMN low_stock_threshold INT NOT NULL DEFAULT 10 COMMENT '低库存预警阈值' AFTER stock_status,
    ADD COLUMN high_stock_threshold INT NOT NULL DEFAULT 1000 COMMENT '高库存预警阈值' AFTER low_stock_threshold,
    ADD COLUMN warning_status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '库存预警状态' AFTER high_stock_threshold;

UPDATE ims_stock
SET stock_status = 'ACTIVE',
    low_stock_threshold = 10,
    high_stock_threshold = GREATEST(total_stock, 1000),
    warning_status = CASE
        WHEN available_stock <= 10 THEN 'LOW'
        WHEN available_stock >= GREATEST(total_stock, 1000) THEN 'HIGH'
        ELSE 'NORMAL'
    END;
