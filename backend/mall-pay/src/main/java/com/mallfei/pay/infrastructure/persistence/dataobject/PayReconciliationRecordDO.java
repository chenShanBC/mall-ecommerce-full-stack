package com.mallfei.pay.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("pay_reconciliation_record")
public class PayReconciliationRecordDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchNo;
    private String bizType;
    private String orderNo;
    private String payOrderNo;
    private String refundNo;
    private String localStatus;
    private String channelStatus;
    private Long localAmountCent;
    private Long channelAmountCent;
    private Boolean consistent;
    private String diffType;
    private String repairStatus;
    private String remark;
    private LocalDateTime repairedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getPayOrderNo() { return payOrderNo; }
    public void setPayOrderNo(String payOrderNo) { this.payOrderNo = payOrderNo; }
    public String getRefundNo() { return refundNo; }
    public void setRefundNo(String refundNo) { this.refundNo = refundNo; }
    public String getLocalStatus() { return localStatus; }
    public void setLocalStatus(String localStatus) { this.localStatus = localStatus; }
    public String getChannelStatus() { return channelStatus; }
    public void setChannelStatus(String channelStatus) { this.channelStatus = channelStatus; }
    public Long getLocalAmountCent() { return localAmountCent; }
    public void setLocalAmountCent(Long localAmountCent) { this.localAmountCent = localAmountCent; }
    public Long getChannelAmountCent() { return channelAmountCent; }
    public void setChannelAmountCent(Long channelAmountCent) { this.channelAmountCent = channelAmountCent; }
    public Boolean getConsistent() { return consistent; }
    public void setConsistent(Boolean consistent) { this.consistent = consistent; }
    public String getDiffType() { return diffType; }
    public void setDiffType(String diffType) { this.diffType = diffType; }
    public String getRepairStatus() { return repairStatus; }
    public void setRepairStatus(String repairStatus) { this.repairStatus = repairStatus; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getRepairedAt() { return repairedAt; }
    public void setRepairedAt(LocalDateTime repairedAt) { this.repairedAt = repairedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
