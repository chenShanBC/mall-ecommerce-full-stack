package com.mallfei.order.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallfei.order.infrastructure.persistence.dataobject.OrderItemDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemDO> {

    @Insert("""
            <script>
            INSERT INTO oms_order_item(order_id, order_no, sku_id, spu_id, sku_name, sku_image_url, sale_price_cent, quantity, total_amount_cent)
            VALUES
            <foreach collection="items" item="item" separator=",">
                (#{item.orderId}, #{item.orderNo}, #{item.skuId}, #{item.spuId}, #{item.skuName}, #{item.skuImageUrl}, #{item.salePriceCent}, #{item.quantity}, #{item.totalAmountCent})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("items") List<OrderItemDO> items);
}
