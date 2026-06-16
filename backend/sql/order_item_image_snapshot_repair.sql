-- 修复历史订单明细商品图片快照为空的问题。
-- 订单明细使用 SKU 所属 SPU 的主图作为下单快照图，修复后 H5 订单列表/详情可展示真实商品图。

UPDATE oms_order_item oi
JOIN pms_sku sku ON sku.id = oi.sku_id AND sku.deleted_at IS NULL
JOIN pms_spu spu ON spu.id = sku.spu_id AND spu.deleted_at IS NULL
SET oi.sku_image_url = spu.main_image_url
WHERE (oi.sku_image_url IS NULL OR oi.sku_image_url = '')
  AND spu.main_image_url IS NOT NULL
  AND spu.main_image_url <> '';
