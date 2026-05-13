package com.mallfei.stock.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("ims_stock_operation_log")
public class StockOperationLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long skuId;
    private String operationType;
    private String businessType;
    private String businessNo;
    private Integer changeQuantity;
    private Integer beforeTotalStock;
    private Integer beforeLockedStock;
    private Integer beforeAvailableStock;
    private Integer afterTotalStock;
    private Integer afterLockedStock;
    private Integer afterAvailableStock;
    private String remark;
    private String operatorType;
    private Long operatorId;
    private String operatorName;
    private String sourceType;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getBusinessNo() { return businessNo; }
    public void setBusinessNo(String businessNo) { this.businessNo = businessNo; }
    public Integer getChangeQuantity() { return changeQuantity; }
    public void setChangeQuantity(Integer changeQuantity) { this.changeQuantity = changeQuantity; }
    public Integer getBeforeTotalStock() { return beforeTotalStock; }
    public void setBeforeTotalStock(Integer beforeTotalStock) { this.beforeTotalStock = beforeTotalStock; }
    public Integer getBeforeLockedStock() { return beforeLockedStock; }
    public void setBeforeLockedStock(Integer beforeLockedStock) { this.beforeLockedStock = beforeLockedStock; }
    public Integer getBeforeAvailableStock() { return beforeAvailableStock; }
    public void setBeforeAvailableStock(Integer beforeAvailableStock) { this.beforeAvailableStock = beforeAvailableStock; }
    public Integer getAfterTotalStock() { return afterTotalStock; }
    public void setAfterTotalStock(Integer afterTotalStock) { this.afterTotalStock = afterTotalStock; }
    public Integer getAfterLockedStock() { return afterLockedStock; }
    public void setAfterLockedStock(Integer afterLockedStock) { this.afterLockedStock = afterLockedStock; }
    public Integer getAfterAvailableStock() { return afterAvailableStock; }
    public void setAfterAvailableStock(Integer afterAvailableStock) { this.afterAvailableStock = afterAvailableStock; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getOperatorType() { return operatorType; }
    public void setOperatorType(String operatorType) { this.operatorType = operatorType; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
