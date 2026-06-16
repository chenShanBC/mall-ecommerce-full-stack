package com.mallfei.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(name = "OrderRefundApplyRequest", description = "订单退款申请请求；items为空表示整单退款")
public record OrderRefundApplyRequest(
        @Schema(description = "退款原因", example = "商品与描述不符")
        @NotBlank(message = "退款原因不能为空")
        @Size(max = 200, message = "退款原因不能超过200个字符")
        String reason,

        @Schema(description = "部分退款明细；为空表示整单退款")
        @Valid
        List<Item> items
) {
    @Schema(name = "OrderRefundApplyRequestItem", description = "订单退款明细")
    public record Item(
            @Schema(description = "订单明细ID", example = "1")
            Long orderItemId,
            @Schema(description = "SKU ID", example = "1001")
            Long skuId,
            @Schema(description = "退款数量", example = "1")
            @Min(value = 1, message = "退款数量必须大于0")
            Integer quantity
    ) {
    }
}
