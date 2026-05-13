SET @sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'ims_stock' AND index_name = 'idx_ims_stock_status_warning_sku'
    ),
    'DROP INDEX idx_ims_stock_status_warning_sku ON ims_stock',
    'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'ims_stock' AND index_name = 'idx_ims_stock_warning_status_sku'
    ),
    'DROP INDEX idx_ims_stock_warning_status_sku ON ims_stock',
    'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'ims_stock' AND index_name = 'idx_ims_stock_stock_status_sku'
    ),
    'DROP INDEX idx_ims_stock_stock_status_sku ON ims_stock',
    'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'pms_spu' AND index_name = 'idx_pms_spu_status_category_deleted_id'
    ),
    'DROP INDEX idx_pms_spu_status_category_deleted_id ON pms_spu',
    'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'pms_spu' AND index_name = 'idx_pms_spu_category_deleted_id'
    ),
    'DROP INDEX idx_pms_spu_category_deleted_id ON pms_spu',
    'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'pms_spu' AND index_name = 'idx_pms_spu_deleted_id'
    ),
    'DROP INDEX idx_pms_spu_deleted_id ON pms_spu',
    'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'pms_sku' AND index_name = 'idx_pms_sku_spu_deleted_id'
    ),
    'DROP INDEX idx_pms_sku_spu_deleted_id ON pms_sku',
    'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'pms_sku' AND index_name = 'idx_pms_sku_deleted_sku_code'
    ),
    'DROP INDEX idx_pms_sku_deleted_sku_code ON pms_sku',
    'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'oms_order' AND index_name = 'idx_oms_order_status_id'
    ),
    'DROP INDEX idx_oms_order_status_id ON oms_order',
    'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'oms_order' AND index_name = 'idx_oms_order_user_id_id'
    ),
    'DROP INDEX idx_oms_order_user_id_id ON oms_order',
    'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'oms_order' AND index_name = 'idx_oms_order_order_no'
    ),
    'DROP INDEX idx_oms_order_order_no ON oms_order',
    'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'oms_order' AND index_name = 'idx_oms_order_receiver_phone'
    ),
    'DROP INDEX idx_oms_order_receiver_phone ON oms_order',
    'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'pay_order' AND index_name = 'idx_pay_order_status_id'
    ),
    'DROP INDEX idx_pay_order_status_id ON pay_order',
    'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'pay_order' AND index_name = 'idx_pay_order_order_no'
    ),
    'DROP INDEX idx_pay_order_order_no ON pay_order',
    'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'pay_order' AND index_name = 'idx_pay_order_pay_order_no'
    ),
    'DROP INDEX idx_pay_order_pay_order_no ON pay_order',
    'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE INDEX idx_ims_stock_status_warning_sku ON ims_stock (stock_status, warning_status, sku_id);
CREATE INDEX idx_ims_stock_warning_status_sku ON ims_stock (warning_status, sku_id);
CREATE INDEX idx_ims_stock_stock_status_sku ON ims_stock (stock_status, sku_id);

CREATE INDEX idx_pms_spu_status_category_deleted_id ON pms_spu (status, category_id, deleted_at, id);
CREATE INDEX idx_pms_spu_category_deleted_id ON pms_spu (category_id, deleted_at, id);
CREATE INDEX idx_pms_spu_deleted_id ON pms_spu (deleted_at, id);

CREATE INDEX idx_pms_sku_spu_deleted_id ON pms_sku (spu_id, deleted_at, id);
CREATE INDEX idx_pms_sku_deleted_sku_code ON pms_sku (deleted_at, sku_code);

CREATE INDEX idx_oms_order_status_id ON oms_order (order_status, id);
CREATE INDEX idx_oms_order_user_id_id ON oms_order (user_id, id);
CREATE INDEX idx_oms_order_order_no ON oms_order (order_no);
CREATE INDEX idx_oms_order_receiver_phone ON oms_order (receiver_phone);

CREATE INDEX idx_pay_order_status_id ON pay_order (pay_status, id);
CREATE INDEX idx_pay_order_order_no ON pay_order (order_no);
CREATE INDEX idx_pay_order_pay_order_no ON pay_order (pay_order_no);
