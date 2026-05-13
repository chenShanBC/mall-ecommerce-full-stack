package com.mallfei.product.domain.model;

import com.mallfei.common.exception.BusinessException;

import java.util.List;

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

    public boolean online() {
        return "ONLINE".equalsIgnoreCase(status);
    }

    public ProductSku defaultSku() {
        return skus.stream().findFirst()
                .orElseThrow(() -> BusinessException.badRequest("商品SKU不存在"));
    }

    public ProductSpu applyUpdate(String name,
                                  Long categoryId,
                                  String mainImageUrl,
                                  String description,
                                  String status,
                                  List<ProductSku> skus) {
        return new ProductSpu(
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
}
