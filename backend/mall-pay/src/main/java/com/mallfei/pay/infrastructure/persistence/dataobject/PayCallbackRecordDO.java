package com.mallfei.pay.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("pay_callback_record")
public class PayCallbackRecordDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String channel;
    private String callbackType;
    private String payOrderNo;
    private String refundNo;
    private String orderNo;
    private String outTradeNo;
    private String transactionNo;
    private Long amountCent;
    private String tradeStatus;
    private String signature;
    private Boolean verified;
    private String processStatus;
    private String failReason;
    private String rawPayload;
    private LocalDateTime callbackTime;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getCallbackType() { return callbackType; }
    public void setCallbackType(String callbackType) { this.callbackType = callbackType; }
    public String getPayOrderNo() { return payOrderNo; }
    public void setPayOrderNo(String payOrderNo) { this.payOrderNo = payOrderNo; }
    public String getRefundNo() { return refundNo; }
    public void setRefundNo(String refundNo) { this.refundNo = refundNo; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getOutTradeNo() { return outTradeNo; }
    public void setOutTradeNo(String outTradeNo) { this.outTradeNo = outTradeNo; }
    public String getTransactionNo() { return transactionNo; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }
    public Long getAmountCent() { return amountCent; }
    public void setAmountCent(Long amountCent) { this.amountCent = amountCent; }
    public String getTradeStatus() { return tradeStatus; }
    public void setTradeStatus(String tradeStatus) { this.tradeStatus = tradeStatus; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    public String getProcessStatus() { return processStatus; }
    public void setProcessStatus(String processStatus) { this.processStatus = processStatus; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }
    public LocalDateTime getCallbackTime() { return callbackTime; }
    public void setCallbackTime(LocalDateTime callbackTime) { this.callbackTime = callbackTime; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
