package com.mallfei.stock.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("ims_stock_lock")
public class StockLockRecordDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String lockNo;
    private Long skuId;
    private String businessType;
    private String businessNo;
    private Integer quantity;
    private String status;
    private LocalDateTime lockTime;
    private LocalDateTime releaseTime;
    private LocalDateTime deductTime;
    private Boolean reservedSynced;
    private Boolean cancelledSynced;
    private Boolean confirmedSynced;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLockNo() { return lockNo; }
    public void setLockNo(String lockNo) { this.lockNo = lockNo; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getBusinessNo() { return businessNo; }
    public void setBusinessNo(String businessNo) { this.businessNo = businessNo; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLockTime() { return lockTime; }
    public void setLockTime(LocalDateTime lockTime) { this.lockTime = lockTime; }
    public LocalDateTime getReleaseTime() { return releaseTime; }
    public void setReleaseTime(LocalDateTime releaseTime) { this.releaseTime = releaseTime; }
    public LocalDateTime getDeductTime() { return deductTime; }
    public void setDeductTime(LocalDateTime deductTime) { this.deductTime = deductTime; }
    public Boolean getReservedSynced() { return reservedSynced; }
    public void setReservedSynced(Boolean reservedSynced) { this.reservedSynced = reservedSynced; }
    public Boolean getCancelledSynced() { return cancelledSynced; }
    public void setCancelledSynced(Boolean cancelledSynced) { this.cancelledSynced = cancelledSynced; }
    public Boolean getConfirmedSynced() { return confirmedSynced; }
    public void setConfirmedSynced(Boolean confirmedSynced) { this.confirmedSynced = confirmedSynced; }
}
