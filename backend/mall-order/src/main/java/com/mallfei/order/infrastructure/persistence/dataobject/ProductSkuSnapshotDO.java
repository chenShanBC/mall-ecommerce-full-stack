package com.mallfei.order.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("pms_sku")
public class ProductSkuSnapshotDO {

    private Long id;
    private Long spuId;
    private String skuName;
    private Long salePriceCent;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }
    public Long getSalePriceCent() { return salePriceCent; }
    public void setSalePriceCent(Long salePriceCent) { this.salePriceCent = salePriceCent; }
}
