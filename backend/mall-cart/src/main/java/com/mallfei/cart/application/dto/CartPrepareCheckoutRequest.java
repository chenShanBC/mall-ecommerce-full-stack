package com.mallfei.cart.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(name = "CartPrepareCheckoutRequest", description = "购物车结算前校验请求")
public record CartPrepareCheckoutRequest(
        @Schema(description = "待结算购物车项ID列表")
        @NotEmpty(message = "请选择要结算的购物车项") List<Long> cartItemIds
) {
}
