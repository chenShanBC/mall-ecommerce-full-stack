package com.mallfei.cart.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CartCheckoutRequest", description = "购物车结算下单请求")
public record CartCheckoutRequest(
        @Schema(description = "收货人姓名", example = "Cart User")
        @NotBlank(message = "收货人不能为空") String receiverName,
        @Schema(description = "收货人电话", example = "13800138000")
        @NotBlank(message = "收货电话不能为空") String receiverPhone,
        @Schema(description = "收货省份", example = "北京市")
        @NotBlank(message = "省不能为空") String receiverProvinceName,
        @Schema(description = "收货城市", example = "北京市")
        @NotBlank(message = "市不能为空") String receiverCityName,
        @Schema(description = "收货区县", example = "朝阳区")
        @NotBlank(message = "区不能为空") String receiverDistrictName,
        @Schema(description = "详细收货地址", example = "望京 SOHO T2 2002")
        @NotBlank(message = "详细地址不能为空") String receiverDetailAddress,
        @Schema(description = "订单备注", example = "cart checkout test")
        String remark
) {
}
