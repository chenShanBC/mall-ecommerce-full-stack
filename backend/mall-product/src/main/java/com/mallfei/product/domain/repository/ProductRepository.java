package com.mallfei.product.domain.repository;

import com.mallfei.common.api.PageResult;
import com.mallfei.product.domain.model.ProductSpu;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    record SkuSalesIncrement(Long skuId, Integer quantity) {
    }

    List<ProductSpu> findAllOnline();

    Optional<ProductSpu> findOnlineById(Long id);

    List<ProductSpu> findAll();

    PageResult<ProductSpu> search(String keyword, Long categoryId, String status, long page, long size);

    Optional<ProductSpu> findById(Long id);

    Optional<ProductSpu> findBySkuId(Long skuId);

    ProductSpu save(ProductSpu productSpu);

    ProductSpu update(ProductSpu productSpu);

    void updateStatus(Long productId, String status);

    void incrementSkuSales(List<SkuSalesIncrement> items);

    void decrementSkuSales(List<SkuSalesIncrement> items);
}
