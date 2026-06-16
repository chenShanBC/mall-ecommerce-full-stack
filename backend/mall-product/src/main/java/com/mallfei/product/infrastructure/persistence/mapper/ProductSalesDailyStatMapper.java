package com.mallfei.product.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallfei.product.infrastructure.persistence.dataobject.ProductSalesDailyStatDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ProductSalesDailyStatMapper extends BaseMapper<ProductSalesDailyStatDO> {

    @Insert("""
            INSERT INTO pms_product_sales_daily_stat (
                stat_date, spu_id, sku_id, sale_channel, completed_quantity, completed_amount_cent
            ) VALUES (
                #{statDate}, #{spuId}, #{skuId}, #{saleChannel}, #{quantity}, #{amountCent}
            )
            ON DUPLICATE KEY UPDATE
                completed_quantity = completed_quantity + VALUES(completed_quantity),
                completed_amount_cent = completed_amount_cent + VALUES(completed_amount_cent),
                updated_at = NOW()
            """)
    int incrementCompleted(@Param("statDate") LocalDate statDate,
                           @Param("spuId") Long spuId,
                           @Param("skuId") Long skuId,
                           @Param("saleChannel") String saleChannel,
                           @Param("quantity") int quantity,
                           @Param("amountCent") long amountCent);

    @Select("""
            SELECT spu_id AS spuId, COALESCE(SUM(completed_quantity), 0) AS quantity, COALESCE(SUM(completed_amount_cent), 0) AS amountCent
            FROM pms_product_sales_daily_stat
            WHERE stat_date >= #{startDate} AND stat_date <= #{endDate}
            GROUP BY spu_id
            """)
    List<ProductSalesAggregateRow> aggregateBySpu(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT spu_id AS spuId, COALESCE(SUM(completed_quantity), 0) AS quantity, COALESCE(SUM(completed_amount_cent), 0) AS amountCent
            FROM pms_product_sales_daily_stat
            WHERE stat_date >= #{startDate} AND stat_date <= #{endDate}
              AND spu_id IN (${spuIdsSql})
            GROUP BY spu_id
            """)
    List<ProductSalesAggregateRow> aggregateBySpuIds(@Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate,
                                                     @Param("spuIdsSql") String spuIdsSql);

    @Select("""
            SELECT COALESCE(SUM(completed_quantity), 0)
            FROM pms_product_sales_daily_stat
            WHERE stat_date >= #{startDate} AND stat_date <= #{endDate}
            """)
    long sumQuantity(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT COALESCE(SUM(completed_amount_cent), 0)
            FROM pms_product_sales_daily_stat
            WHERE stat_date >= #{startDate} AND stat_date <= #{endDate}
            """)
    long sumAmountCent(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT DATE_FORMAT(stat_date, '%Y-%m') AS month,
                   COALESCE(SUM(completed_quantity), 0) AS quantity,
                   COALESCE(SUM(completed_amount_cent), 0) AS amountCent
            FROM pms_product_sales_daily_stat
            WHERE stat_date >= #{startDate} AND stat_date <= #{endDate}
            GROUP BY DATE_FORMAT(stat_date, '%Y-%m')
            ORDER BY month ASC
            """)
    List<ProductSalesMonthlyAggregateRow> aggregateMonthly(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    class ProductSalesAggregateRow {
        private Long spuId;
        private Integer quantity;
        private Long amountCent;

        public ProductSalesAggregateRow() {
        }

        public Long getSpuId() {
            return spuId;
        }

        public void setSpuId(Long spuId) {
            this.spuId = spuId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Long getAmountCent() {
            return amountCent;
        }

        public void setAmountCent(Long amountCent) {
            this.amountCent = amountCent;
        }
    }

    class ProductSalesMonthlyAggregateRow {
        private String month;
        private Long quantity;
        private Long amountCent;

        public ProductSalesMonthlyAggregateRow() {
        }

        public String getMonth() {
            return month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        public Long getQuantity() {
            return quantity;
        }

        public void setQuantity(Long quantity) {
            this.quantity = quantity;
        }

        public Long getAmountCent() {
            return amountCent;
        }

        public void setAmountCent(Long amountCent) {
            this.amountCent = amountCent;
        }
    }
}
