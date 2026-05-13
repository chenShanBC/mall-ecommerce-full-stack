package com.mallfei.stock.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(name = "StockOperationRequest", description = "库存操作请求")
public record StockOperationRequest(
        @Schema(description = "业务类型", example = "ORDER")
        @NotBlank(message = "业务类型不能为空") String businessType,
        @Schema(description = "业务单号", example = "MANUAL-ORDER-001")
        @NotBlank(message = "业务单号不能为空") String businessNo,
        @Schema(description = "库存操作项列表")
        @NotNull(message = "库存项不能为空") List<Item> items
) {
    @Schema(name = "StockOperationItem", description = "库存操作项")
    public record Item(
            @Schema(description = "SKU ID", example = "1")
            @NotNull(message = "SKU不能为空") Long skuId,
            @Schema(description = "操作数量", example = "1")
            @NotNull(message = "数量不能为空") @Min(value = 1, message = "数量必须大于0") Integer quantity
    ) {
    }
}
