package com.mallfei.product.domain.model;

import com.mallfei.common.exception.BusinessException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ProductSpu(
        Long id,
        String name,
        Long categoryId,
        String mainImageUrl,
        String albumImagesJson,
        String description,
        String status,
        List<ProductSku> skus
) {
    public ProductSpu {
        skus = skus == null ? List.of() : List.copyOf(skus);
    }

    public static ProductSpu create(Long id,
                                    String name,
                                    Long categoryId,
                                    String mainImageUrl,
                                    String albumImagesJson,
                                    String description,
                                    String status,
                                    List<ProductSku> skus) {
        ProductSpu product = new ProductSpu(id, name, categoryId, mainImageUrl, albumImagesJson, description, status, skus);
        product.validateEditable();
        return product;
    }

    public void validateEditable() {
        if (name == null || name.isBlank()) {
            throw BusinessException.badRequest("商品名称不能为空");
        }
        if (categoryId == null) {
            throw BusinessException.badRequest("商品类目不能为空");
        }
        if (mainImageUrl == null || mainImageUrl.isBlank()) {
            throw BusinessException.badRequest("商品主图不能为空");
        }
        if (skus.isEmpty()) {
            throw BusinessException.badRequest("商品至少需要一个SKU");
        }
        if (!"ONLINE".equalsIgnoreCase(status) && !"OFFLINE".equalsIgnoreCase(status)) {
            throw BusinessException.badRequest("商品状态仅支持 ONLINE 或 OFFLINE");
        }
        Set<String> skuCodes = new HashSet<>();
        for (ProductSku sku : skus) {
            if (sku.skuCode() == null || sku.skuCode().isBlank()) {
                throw BusinessException.badRequest("SKU编码不能为空");
            }
            if (!skuCodes.add(sku.skuCode().trim())) {
                throw BusinessException.badRequest("SKU编码不能重复");
            }
        }
        if ("ONLINE".equalsIgnoreCase(status) && skus.stream().noneMatch(ProductSku::enabled)) {
            throw BusinessException.badRequest("商品上架前至少需要一个启用SKU");
        }
    }

    public boolean online() {
        return "ONLINE".equalsIgnoreCase(status);
    }

    public ProductSku defaultSku() {
        return skus.stream()
                .filter(ProductSku::enabled)
                .findFirst()
                .or(() -> skus.stream().findFirst())
                .orElseThrow(() -> BusinessException.badRequest("商品SKU不存在"));
    }

    public ProductSpu applyUpdate(String name,
                                  Long categoryId,
                                  String mainImageUrl,
                                  String description,
                                  String status,
                                  List<ProductSku> skus) {
        return ProductSpu.create(
                id,
                name,
                categoryId,
                mainImageUrl,
                albumImagesJson,
                description,
                status,
                skus
        );
    }

    public ProductSpu applyStatus(String status) {
        ProductSpu candidate = new ProductSpu(
                id,
                name,
                categoryId,
                mainImageUrl,
                albumImagesJson,
                description,
                status,
                skus
        );
        if (!"ONLINE".equalsIgnoreCase(status) && !"OFFLINE".equalsIgnoreCase(status)) {
            throw BusinessException.badRequest("商品状态仅支持 ONLINE 或 OFFLINE");
        }
        if (candidate.online() && candidate.skus().stream().noneMatch(ProductSku::enabled)) {
            throw BusinessException.badRequest("上架失败：至少需要一个启用SKU");
        }
        return candidate;
    }
}
