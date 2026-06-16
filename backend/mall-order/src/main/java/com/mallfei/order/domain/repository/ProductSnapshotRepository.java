package com.mallfei.order.domain.repository;

import com.mallfei.order.domain.model.ProductSnapshot;

import java.util.List;
import java.util.Optional;

public interface ProductSnapshotRepository {

    Optional<ProductSnapshot> findBySkuId(Long skuId);

    List<ProductSnapshot> findBySkuIds(List<Long> skuIds);
}
