package com.mallfei.product.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("pms_product_sales_daily_stat")
public class ProductSalesDailyStatDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Long spuId;
    private Long skuId;
    private String saleChannel;
    private Integer completedQuantity;
    private Long completedAmountCent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public String getSaleChannel() { return saleChannel; }
    public void setSaleChannel(String saleChannel) { this.saleChannel = saleChannel; }
    public Integer getCompletedQuantity() { return completedQuantity; }
    public void setCompletedQuantity(Integer completedQuantity) { this.completedQuantity = completedQuantity; }
    public Long getCompletedAmountCent() { return completedAmountCent; }
    public void setCompletedAmountCent(Long completedAmountCent) { this.completedAmountCent = completedAmountCent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
