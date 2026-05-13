package com.mallfei.aftersale.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "AftersaleRefundApplyRequest", description = "售后仅退款申请请求")
public record AftersaleRefundApplyRequest(
        @Schema(description = "订单ID", example = "1")
        @NotNull(message = "订单ID不能为空")
        Long orderId,
        @Schema(description = "退款原因", example = "商品与描述不符")
        @NotBlank(message = "退款原因不能为空")
        @Size(max = 200, message = "退款原因不能超过200个字符")
        String reason
) {
}
