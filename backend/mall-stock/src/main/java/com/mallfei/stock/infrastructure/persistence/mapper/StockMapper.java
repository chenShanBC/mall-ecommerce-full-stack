package com.mallfei.stock.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallfei.stock.infrastructure.persistence.dataobject.StockDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StockMapper extends BaseMapper<StockDO> {

    @Update("""
            UPDATE ims_stock
            SET available_stock = available_stock - #{quantity},
                locked_stock = locked_stock + #{quantity},
                warning_status = CASE
                    WHEN available_stock - #{quantity} <= low_stock_threshold THEN 'LOW'
                    WHEN available_stock - #{quantity} >= high_stock_threshold THEN 'HIGH'
                    ELSE 'NORMAL'
                END,
                version = version + 1
            WHERE sku_id = #{skuId}
              AND stock_status = 'ACTIVE'
              AND available_stock >= #{quantity}
            """)
    int applyReservedSync(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    @Update("""
            UPDATE ims_stock
            SET available_stock = available_stock + #{quantity},
                locked_stock = locked_stock - #{quantity},
                warning_status = CASE
                    WHEN available_stock + #{quantity} <= low_stock_threshold THEN 'LOW'
                    WHEN available_stock + #{quantity} >= high_stock_threshold THEN 'HIGH'
                    ELSE 'NORMAL'
                END,
                version = version + 1
            WHERE sku_id = #{skuId}
              AND locked_stock >= #{quantity}
            """)
    int applyCancelledSync(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    @Update("""
            UPDATE ims_stock
            SET total_stock = total_stock - #{quantity},
                locked_stock = locked_stock - #{quantity},
                warning_status = CASE
                    WHEN available_stock <= low_stock_threshold THEN 'LOW'
                    WHEN available_stock >= high_stock_threshold THEN 'HIGH'
                    ELSE 'NORMAL'
                END,
                version = version + 1
            WHERE sku_id = #{skuId}
              AND total_stock >= #{quantity}
              AND locked_stock >= #{quantity}
            """)
    int applyConfirmedSync(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    @Update("""
            UPDATE ims_stock
            SET locked_stock = #{lockedStock},
                available_stock = #{availableStock},
                warning_status = #{warningStatus},
                version = version + 1
            WHERE sku_id = #{skuId}
            """)
    int calibrateSnapshot(@Param("skuId") Long skuId, @Param("lockedStock") Integer lockedStock, @Param("availableStock") Integer availableStock, @Param("warningStatus") String warningStatus);
}
