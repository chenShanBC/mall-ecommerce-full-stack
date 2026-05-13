package com.mallfei.cart.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "CartSettlementPreviewView", description = "购物车结算预览视图")
public record CartSettlementPreviewView(
        @Schema(description = "结算商品项列表")
        List<CartSettlementPreviewItemView> items,
        @Schema(description = "合计金额，单位分", example = "19980")
        Long totalAmount,
        @Schema(description = "勾选商品数", example = "2")
        Integer checkedCount
) {
}
