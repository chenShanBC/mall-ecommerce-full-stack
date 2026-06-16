package com.mallfei.order.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("oms_order_refund_item")
public class OrderRefundItemDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long refundId;
    private String refundNo;
    private Long orderItemId;
    private Long skuId;
    private Integer quantity;
    private Long refundAmountCent;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRefundId() { return refundId; }
    public void setRefundId(Long refundId) { this.refundId = refundId; }
    public String getRefundNo() { return refundNo; }
    public void setRefundNo(String refundNo) { this.refundNo = refundNo; }
    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Long getRefundAmountCent() { return refundAmountCent; }
    public void setRefundAmountCent(Long refundAmountCent) { this.refundAmountCent = refundAmountCent; }
}
