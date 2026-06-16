package com.mallfei.product.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallfei.product.infrastructure.persistence.dataobject.ProductSalesStatEventDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductSalesStatEventMapper extends BaseMapper<ProductSalesStatEventDO> {

    @Insert("""
            INSERT IGNORE INTO pms_product_sales_stat_event (event_key, event_type, biz_no)
            VALUES (#{eventKey}, #{eventType}, #{bizNo})
            """)
    int insertIgnore(@Param("eventKey") String eventKey, @Param("eventType") String eventType, @Param("bizNo") String bizNo);
}
