package com.mallfei.cart.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(name = "CartUpdateItemRequest", description = "编辑购物车商品项请求")
public record CartUpdateItemRequest(
        @Schema(description = "目标SKU ID，切换规格时传入", example = "2")
        Long skuId,
        @Schema(description = "购买数量", example = "3")
        @Min(value = 1, message = "数量最少为1") Integer quantity,
        @Schema(description = "是否勾选", example = "true")
        Boolean checked
) {

    public boolean hasNoChanges() {
        return skuId == null && quantity == null && checked == null;
    }
}
