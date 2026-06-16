package com.mallfei.stock.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("ims_stock_reconciliation_record")
public class StockReconciliationRecordDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long skuId;
    private String status;
    private String severity;
    private String stockSnapshotJson;
    private String expectedSnapshotJson;
    private String redisSnapshotJson;
    private String differencesJson;
    private String repairStatus;
    private String repairRemark;
    private LocalDateTime checkedAt;
    private LocalDateTime repairedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getStockSnapshotJson() { return stockSnapshotJson; }
    public void setStockSnapshotJson(String stockSnapshotJson) { this.stockSnapshotJson = stockSnapshotJson; }
    public String getExpectedSnapshotJson() { return expectedSnapshotJson; }
    public void setExpectedSnapshotJson(String expectedSnapshotJson) { this.expectedSnapshotJson = expectedSnapshotJson; }
    public String getRedisSnapshotJson() { return redisSnapshotJson; }
    public void setRedisSnapshotJson(String redisSnapshotJson) { this.redisSnapshotJson = redisSnapshotJson; }
    public String getDifferencesJson() { return differencesJson; }
    public void setDifferencesJson(String differencesJson) { this.differencesJson = differencesJson; }
    public String getRepairStatus() { return repairStatus; }
    public void setRepairStatus(String repairStatus) { this.repairStatus = repairStatus; }
    public String getRepairRemark() { return repairRemark; }
    public void setRepairRemark(String repairRemark) { this.repairRemark = repairRemark; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
    public LocalDateTime getRepairedAt() { return repairedAt; }
    public void setRepairedAt(LocalDateTime repairedAt) { this.repairedAt = repairedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
