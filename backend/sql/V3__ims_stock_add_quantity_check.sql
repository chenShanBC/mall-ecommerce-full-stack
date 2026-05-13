UPDATE ims_stock
SET total_stock = GREATEST(IFNULL(total_stock, 0), 0),
    locked_stock = GREATEST(IFNULL(locked_stock, 0), 0),
    available_stock = GREATEST(IFNULL(available_stock, 0), 0);

UPDATE ims_stock
SET total_stock = locked_stock + available_stock
WHERE total_stock <> locked_stock + available_stock;

ALTER TABLE ims_stock
    ADD CONSTRAINT chk_ims_stock_quantity_relation
    CHECK (total_stock = locked_stock + available_stock);
