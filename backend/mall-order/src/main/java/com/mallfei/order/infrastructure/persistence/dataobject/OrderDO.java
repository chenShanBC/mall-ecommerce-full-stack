package com.mallfei.order.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("oms_order")
public class OrderDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private String orderStatus;
    private Long totalAmountCent;
    private Long payAmountCent;
    private Long freightAmountCent;
    private Long discountAmountCent;
    private String receiverName;
    private String receiverPhone;
    private String receiverProvinceName;
    private String receiverCityName;
    private String receiverDistrictName;
    private String receiverDetailAddress;
    private String remark;
    private String payType;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public Long getTotalAmountCent() { return totalAmountCent; }
    public void setTotalAmountCent(Long totalAmountCent) { this.totalAmountCent = totalAmountCent; }
    public Long getPayAmountCent() { return payAmountCent; }
    public void setPayAmountCent(Long payAmountCent) { this.payAmountCent = payAmountCent; }
    public Long getFreightAmountCent() { return freightAmountCent; }
    public void setFreightAmountCent(Long freightAmountCent) { this.freightAmountCent = freightAmountCent; }
    public Long getDiscountAmountCent() { return discountAmountCent; }
    public void setDiscountAmountCent(Long discountAmountCent) { this.discountAmountCent = discountAmountCent; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getReceiverProvinceName() { return receiverProvinceName; }
    public void setReceiverProvinceName(String receiverProvinceName) { this.receiverProvinceName = receiverProvinceName; }
    public String getReceiverCityName() { return receiverCityName; }
    public void setReceiverCityName(String receiverCityName) { this.receiverCityName = receiverCityName; }
    public String getReceiverDistrictName() { return receiverDistrictName; }
    public void setReceiverDistrictName(String receiverDistrictName) { this.receiverDistrictName = receiverDistrictName; }
    public String getReceiverDetailAddress() { return receiverDetailAddress; }
    public void setReceiverDetailAddress(String receiverDetailAddress) { this.receiverDetailAddress = receiverDetailAddress; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getPayType() { return payType; }
    public void setPayType(String payType) { this.payType = payType; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
