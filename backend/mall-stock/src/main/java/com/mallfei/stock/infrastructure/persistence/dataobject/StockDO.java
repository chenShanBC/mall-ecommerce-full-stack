package com.mallfei.stock.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("ims_stock")
public class StockDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long skuId;
    private Integer totalStock;
    private Integer lockedStock;
    private Integer availableStock;
    private String stockStatus;
    private Integer lowStockThreshold;
    private Integer highStockThreshold;
    private String warningStatus;
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Integer getTotalStock() { return totalStock; }
    public void setTotalStock(Integer totalStock) { this.totalStock = totalStock; }
    public Integer getLockedStock() { return lockedStock; }
    public void setLockedStock(Integer lockedStock) { this.lockedStock = lockedStock; }
    public Integer getAvailableStock() { return availableStock; }
    public void setAvailableStock(Integer availableStock) { this.availableStock = availableStock; }
    public String getStockStatus() { return stockStatus; }
    public void setStockStatus(String stockStatus) { this.stockStatus = stockStatus; }
    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public Integer getHighStockThreshold() { return highStockThreshold; }
    public void setHighStockThreshold(Integer highStockThreshold) { this.highStockThreshold = highStockThreshold; }
    public String getWarningStatus() { return warningStatus; }
    public void setWarningStatus(String warningStatus) { this.warningStatus = warningStatus; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
