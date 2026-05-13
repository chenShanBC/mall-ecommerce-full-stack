package com.mallfei.cart.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(name = "CartAddItemRequest", description = "加入购物车请求")
public record CartAddItemRequest(
        @Schema(description = "SKU ID", example = "1")
        @NotNull(message = "SKU不能为空") Long skuId,
        @Schema(description = "购买数量", example = "2")
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量最少为1") Integer quantity,
        @Schema(description = "是否默认勾选", example = "true")
        Boolean checked
) {
}
