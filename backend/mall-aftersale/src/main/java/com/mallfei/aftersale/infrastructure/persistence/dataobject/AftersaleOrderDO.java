package com.mallfei.aftersale.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("aftersale_order")
public class AftersaleOrderDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String aftersaleNo;
    private String orderNo;
    private Long userId;
    private String aftersaleType;
    private String status;
    private String originOrderStatus;
    private Long refundAmountCent;
    private String reason;
    private String rejectReason;
    private String refundNo;
    private String failReason;
    private Integer version;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAftersaleNo() { return aftersaleNo; }
    public void setAftersaleNo(String aftersaleNo) { this.aftersaleNo = aftersaleNo; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAftersaleType() { return aftersaleType; }
    public void setAftersaleType(String aftersaleType) { this.aftersaleType = aftersaleType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOriginOrderStatus() { return originOrderStatus; }
    public void setOriginOrderStatus(String originOrderStatus) { this.originOrderStatus = originOrderStatus; }
    public Long getRefundAmountCent() { return refundAmountCent; }
    public void setRefundAmountCent(Long refundAmountCent) { this.refundAmountCent = refundAmountCent; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public String getRefundNo() { return refundNo; }
    public void setRefundNo(String refundNo) { this.refundNo = refundNo; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
