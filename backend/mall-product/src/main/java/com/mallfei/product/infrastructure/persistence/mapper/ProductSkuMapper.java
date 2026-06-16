package com.mallfei.product.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallfei.product.infrastructure.persistence.dataobject.ProductSkuDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSkuDO> {

    @Update("UPDATE pms_sku SET sales_count = COALESCE(sales_count, 0) + #{quantity} WHERE id = #{skuId} AND deleted_at IS NULL")
    int incrementSales(@Param("skuId") Long skuId, @Param("quantity") int quantity);

    @Update("UPDATE pms_sku SET sales_count = GREATEST(COALESCE(sales_count, 0) - #{quantity}, 0) WHERE id = #{skuId} AND deleted_at IS NULL")
    int decrementSales(@Param("skuId") Long skuId, @Param("quantity") int quantity);
}
