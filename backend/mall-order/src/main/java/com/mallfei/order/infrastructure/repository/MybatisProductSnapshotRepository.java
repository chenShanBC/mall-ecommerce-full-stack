package com.mallfei.order.infrastructure.repository;

import com.mallfei.order.domain.model.ProductSnapshot;
import com.mallfei.order.domain.repository.ProductSnapshotRepository;
import com.mallfei.order.infrastructure.persistence.dataobject.ProductSkuSnapshotDO;
import com.mallfei.order.infrastructure.persistence.mapper.ProductSkuSnapshotMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisProductSnapshotRepository implements ProductSnapshotRepository {

    private final ProductSkuSnapshotMapper productSkuSnapshotMapper;

    public MybatisProductSnapshotRepository(ProductSkuSnapshotMapper productSkuSnapshotMapper) {
        this.productSkuSnapshotMapper = productSkuSnapshotMapper;
    }

    @Override
    public Optional<ProductSnapshot> findBySkuId(Long skuId) {
        return findBySkuIds(List.of(skuId)).stream().findFirst();
    }

    @Override
    public List<ProductSnapshot> findBySkuIds(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }
        return productSkuSnapshotMapper.selectSnapshotBySkuIds(skuIds).stream()
                .map(this::toSnapshot)
                .toList();
    }

    private ProductSnapshot toSnapshot(ProductSkuSnapshotDO sku) {
        return new ProductSnapshot(sku.getId(), sku.getSpuId(), sku.getSkuName(), sku.getMainImageUrl(), sku.getSalePriceCent());
    }
}
