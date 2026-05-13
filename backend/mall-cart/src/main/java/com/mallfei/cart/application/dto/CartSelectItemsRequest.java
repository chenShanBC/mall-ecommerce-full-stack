package com.mallfei.cart.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(name = "CartSelectItemsRequest", description = "设置购物车项勾选状态请求")
public record CartSelectItemsRequest(
        @Schema(description = "购物车项ID列表")
        @NotEmpty(message = "购物车项不能为空") List<Long> cartItemIds,
        @Schema(description = "是否勾选", example = "true")
        @NotNull(message = "勾选状态不能为空") Boolean checked
) {
}
