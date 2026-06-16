package com.mallfei.order.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallfei.order.infrastructure.persistence.dataobject.ProductSkuSnapshotDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductSkuSnapshotMapper extends BaseMapper<ProductSkuSnapshotDO> {

    @Select("""
            <script>
            SELECT sku.id,
                   sku.spu_id,
                   sku.sku_name,
                   sku.sale_price_cent,
                   COALESCE(spu.main_image_url, '') AS main_image_url
            FROM pms_sku sku
            LEFT JOIN pms_spu spu ON spu.id = sku.spu_id AND spu.deleted_at IS NULL
            WHERE sku.deleted_at IS NULL
              AND sku.id IN
              <foreach collection='skuIds' item='skuId' open='(' separator=',' close=')'>
                #{skuId}
              </foreach>
            ORDER BY sku.id ASC
            </script>
            """)
    List<ProductSkuSnapshotDO> selectSnapshotBySkuIds(@Param("skuIds") List<Long> skuIds);
}
