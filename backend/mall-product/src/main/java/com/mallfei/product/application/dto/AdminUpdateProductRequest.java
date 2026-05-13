package com.mallfei.product.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

@Schema(name = "AdminUpdateProductRequest", description = "修改商品请求")
public record AdminUpdateProductRequest(
        @Schema(description = "商品名称", example = "手测新增商品BBBBBBBBBBBBBBBBBBB")
        @NotBlank(message = "商品名称不能为空") String name,
        @Schema(description = "类目ID", example = "1")
        @NotNull(message = "类目不能为空") Long categoryId,
        @Schema(description = "主图地址", example = "https://example.com/product-a.png")
        @NotBlank(message = "主图不能为空") String mainImageUrl,
        @Schema(description = "商品描述", example = "用于手工测试的商品")
        String description,
        @Schema(description = "商品状态", example = "OFFLINE", allowableValues = {"ONLINE", "OFFLINE"})
        @Pattern(regexp = "ONLINE|OFFLINE", message = "商品状态仅支持 ONLINE 或 OFFLINE")
        @NotBlank(message = "商品状态不能为空") String status,
        @Schema(description = "SKU列表")
        @NotNull(message = "SKU列表不能为空") List<@Valid SkuInput> skus
) {
    @Schema(name = "AdminUpdateProductSkuInput", description = "修改商品SKU请求")
    public record SkuInput(
            @Schema(description = "SKU ID，更新已有SKU时必填，新增SKU时可不传", example = "12")
            Long id,
            @Schema(description = "SKU编码", example = "TEST-SKU-001")
            @NotBlank(message = "SKU编码不能为空") String skuCode,
            @Schema(description = "SKU名称", example = "手测商品A-默认规格")
            @NotBlank(message = "SKU名称不能为空") String skuName,
            @Schema(description = "规格JSON", example = "{\"color\":\"black\"}")
            String specJson,
            @Schema(description = "销售价，单位分", example = "9990")
            @NotNull(message = "销售价不能为空") @Min(value = 1, message = "销售价必须大于0") Long salePriceCent,
            @Schema(description = "原价，单位分", example = "12990")
            @NotNull(message = "原价不能为空") @Min(value = 1, message = "原价必须大于0") Long originPriceCent,
            @Schema(description = "SKU状态", example = "OFFLINE", allowableValues = {"ONLINE", "OFFLINE"})
            @Pattern(regexp = "ONLINE|OFFLINE", message = "SKU状态仅支持 ONLINE 或 OFFLINE")
            @NotBlank(message = "SKU状态不能为空") String status,
            @Schema(description = "初始库存，仅新增SKU或重置库存时使用", example = "100")
            @NotNull(message = "初始库存不能为空") @Min(value = 0, message = "初始库存不能小于0") Integer initialStock
    ) {
    }
}
