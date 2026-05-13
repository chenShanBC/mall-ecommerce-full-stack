package com.mallfei.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "OrderRefundApplyRequest", description = "订单退款申请请求")
public record OrderRefundApplyRequest(
        @Schema(description = "退款原因", example = "商品与描述不符")
        @NotBlank(message = "退款原因不能为空")
        @Size(max = 200, message = "退款原因不能超过200个字符")
        String reason
) {
}
