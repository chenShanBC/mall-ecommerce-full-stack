package com.mallfei.order.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallfei.order.domain.model.ProductSnapshot;
import com.mallfei.order.domain.repository.ProductSnapshotRepository;
import com.mallfei.order.infrastructure.persistence.dataobject.ProductSkuSnapshotDO;
import com.mallfei.order.infrastructure.persistence.mapper.ProductSkuSnapshotMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MybatisProductSnapshotRepository implements ProductSnapshotRepository {

    private final ProductSkuSnapshotMapper productSkuSnapshotMapper;

    public MybatisProductSnapshotRepository(ProductSkuSnapshotMapper productSkuSnapshotMapper) {
        this.productSkuSnapshotMapper = productSkuSnapshotMapper;
    }

    @Override
    public Optional<ProductSnapshot> findBySkuId(Long skuId) {
        ProductSkuSnapshotDO skuDO = productSkuSnapshotMapper.selectOne(new LambdaQueryWrapper<ProductSkuSnapshotDO>()
                .eq(ProductSkuSnapshotDO::getId, skuId)
                .last("limit 1"));
        return Optional.ofNullable(skuDO)
                .map(sku -> new ProductSnapshot(sku.getId(), sku.getSpuId(), sku.getSkuName(), "", sku.getSalePriceCent()));
    }
}
