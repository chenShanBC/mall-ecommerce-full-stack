package com.mallfei.product.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallfei.common.api.PageResult;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.product.domain.model.ProductSku;
import com.mallfei.product.domain.model.ProductSpu;
import com.mallfei.product.domain.repository.ProductRepository;
import com.mallfei.product.infrastructure.persistence.dataobject.ProductSkuDO;
import com.mallfei.product.infrastructure.persistence.dataobject.ProductSpuDO;
import com.mallfei.product.infrastructure.persistence.mapper.ProductSkuMapper;
import com.mallfei.product.infrastructure.persistence.mapper.ProductSpuMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class MybatisProductRepository implements ProductRepository {
    private final ProductSpuMapper productSpuMapper;
    private final ProductSkuMapper productSkuMapper;

    public MybatisProductRepository(ProductSpuMapper productSpuMapper, ProductSkuMapper productSkuMapper) {
        this.productSpuMapper = productSpuMapper;
        this.productSkuMapper = productSkuMapper;
    }

    @Override
    public List<ProductSpu> findAllOnline() {
        return productSpuMapper.selectList(new LambdaQueryWrapper<ProductSpuDO>().eq(ProductSpuDO::getStatus, "ONLINE").isNull(ProductSpuDO::getDeletedAt).orderByDesc(ProductSpuDO::getId)).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ProductSpu> findOnlineById(Long id) {
        ProductSpuDO productSpuDO = productSpuMapper.selectOne(new LambdaQueryWrapper<ProductSpuDO>().eq(ProductSpuDO::getId, id).eq(ProductSpuDO::getStatus, "ONLINE").isNull(ProductSpuDO::getDeletedAt).last("limit 1"));
        return Optional.ofNullable(productSpuDO).map(this::toDomain);
    }

    @Override
    public List<ProductSpu> findAll() {
        return productSpuMapper.selectList(new LambdaQueryWrapper<ProductSpuDO>().isNull(ProductSpuDO::getDeletedAt).orderByDesc(ProductSpuDO::getId)).stream().map(this::toDomain).toList();
    }

    @Override
    public PageResult<ProductSpu> search(String keyword, Long categoryId, String status, long page, long size) {
        LambdaQueryWrapper<ProductSpuDO> wrapper = new LambdaQueryWrapper<ProductSpuDO>().isNull(ProductSpuDO::getDeletedAt).orderByDesc(ProductSpuDO::getId);
        if (keyword != null && !keyword.isBlank()) wrapper.like(ProductSpuDO::getName, keyword.trim());
        if (categoryId != null) wrapper.eq(ProductSpuDO::getCategoryId, categoryId);
        if (status != null && !status.isBlank()) wrapper.eq(ProductSpuDO::getStatus, status);
        Page<ProductSpuDO> result = productSpuMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)), wrapper);
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(), result.getRecords().stream().map(this::toDomain).toList());
    }

    @Override
    public Optional<ProductSpu> findById(Long id) {
        ProductSpuDO productSpuDO = productSpuMapper.selectOne(new LambdaQueryWrapper<ProductSpuDO>().eq(ProductSpuDO::getId, id).isNull(ProductSpuDO::getDeletedAt).last("limit 1"));
        return Optional.ofNullable(productSpuDO).map(this::toDomain);
    }

    @Override
    public Optional<ProductSpu> findBySkuId(Long skuId) {
        ProductSkuDO skuDO = productSkuMapper.selectOne(new LambdaQueryWrapper<ProductSkuDO>().eq(ProductSkuDO::getId, skuId).isNull(ProductSkuDO::getDeletedAt).last("limit 1"));
        if (skuDO == null || skuDO.getSpuId() == null) return Optional.empty();
        return findById(skuDO.getSpuId());
    }

    @Override
    public long countByCategoryId(Long categoryId) {
        if (categoryId == null) return 0;
        return productSpuMapper.selectCount(new LambdaQueryWrapper<ProductSpuDO>()
                .eq(ProductSpuDO::getCategoryId, categoryId)
                .isNull(ProductSpuDO::getDeletedAt));
    }

    @Override
    @Transactional
    public ProductSpu save(ProductSpu productSpu) {
        ProductSpuDO spuDO = toSpuDO(productSpu);
        productSpuMapper.insert(spuDO);
        insertSkus(spuDO.getId(), productSpu.skus());
        return toDomain(spuDO);
    }

    @Override
    @Transactional
    public ProductSpu update(ProductSpu productSpu) {
        ProductSpuDO spuDO = toSpuDO(productSpu); spuDO.setId(productSpu.id()); productSpuMapper.updateById(spuDO);
        List<ProductSkuDO> existingSkus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuDO>().eq(ProductSkuDO::getSpuId, productSpu.id()));
        Map<Long, ProductSkuDO> existingSkuById = existingSkus.stream().filter(s -> s.getId() != null).collect(Collectors.toMap(ProductSkuDO::getId, Function.identity()));
        Map<String, ProductSkuDO> activeSkuByCode = existingSkus.stream().filter(s -> s.getDeletedAt() == null).collect(Collectors.toMap(ProductSkuDO::getSkuCode, Function.identity(), (l, r) -> l, LinkedHashMap::new));
        Map<String, ProductSkuDO> deletedSkuByCode = existingSkus.stream().filter(s -> s.getDeletedAt() != null).collect(Collectors.toMap(ProductSkuDO::getSkuCode, Function.identity(), (l, r) -> l, LinkedHashMap::new));
        validateIncomingSkus(productSpu, existingSkuById, activeSkuByCode);
        Set<Long> incomingSkuIds = productSpu.skus().stream().map(ProductSku::id).filter(id -> id != null).collect(Collectors.toSet());
        for (ProductSku sku : productSpu.skus()) {
            if (sku.id() != null) { ProductSkuDO m = existingSkuById.get(sku.id()); m.setSkuCode(sku.skuCode()); m.setSkuName(sku.skuName()); m.setSpecJson(sku.specJson()); m.setSalePriceCent(sku.salePriceCent()); m.setOriginPriceCent(sku.originPriceCent()); m.setSalesCount(sku.salesCount()); m.setStatus(sku.status()); m.setDeletedAt(null); productSkuMapper.updateById(m); continue; }
            ProductSkuDO d = deletedSkuByCode.get(sku.skuCode());
            if (d != null) { d.setSkuCode(sku.skuCode()); d.setSkuName(sku.skuName()); d.setSpecJson(sku.specJson()); d.setSalePriceCent(sku.salePriceCent()); d.setOriginPriceCent(sku.originPriceCent()); d.setSalesCount(sku.salesCount()); d.setStatus(sku.status()); d.setDeletedAt(null); productSkuMapper.updateById(d); incomingSkuIds.add(d.getId()); continue; }
            insertSku(productSpu.id(), sku);
        }
        for (ProductSkuDO existingSku : existingSkus) if (!incomingSkuIds.contains(existingSku.getId())) { existingSku.setDeletedAt(LocalDateTime.now()); productSkuMapper.updateById(existingSku); }
        return findById(productSpu.id()).orElseThrow();
    }

    @Override
    public void updateStatus(Long productId, String status) { ProductSpuDO spuDO = new ProductSpuDO(); spuDO.setId(productId); spuDO.setStatus(status); productSpuMapper.updateById(spuDO); }

    @Override
    @Transactional
    public void incrementSkuSales(List<SkuSalesIncrement> items) {
        for (SkuSalesIncrement item : items) {
            if (item.skuId() == null || item.quantity() == null || item.quantity() <= 0) {
                continue;
            }
            productSkuMapper.incrementSales(item.skuId(), item.quantity());
        }
    }

    @Override
    @Transactional
    public void decrementSkuSales(List<SkuSalesIncrement> items) {
        for (SkuSalesIncrement item : items) {
            if (item.skuId() == null || item.quantity() == null || item.quantity() <= 0) {
                continue;
            }
            productSkuMapper.decrementSales(item.skuId(), item.quantity());
        }
    }

    private void insertSkus(Long spuId, List<ProductSku> skus) { for (ProductSku sku : skus) insertSku(spuId, sku); }

    private void insertSku(Long spuId, ProductSku sku) {
        ProductSkuDO skuDO = new ProductSkuDO();
        skuDO.setSpuId(spuId); skuDO.setSkuCode(sku.skuCode()); skuDO.setSkuName(sku.skuName()); skuDO.setSpecJson(sku.specJson()); skuDO.setSalePriceCent(sku.salePriceCent()); skuDO.setOriginPriceCent(sku.originPriceCent()); skuDO.setSalesCount(sku.salesCount()); skuDO.setStatus(sku.status());
        productSkuMapper.insert(skuDO);
    }

    private void validateIncomingSkus(ProductSpu productSpu, Map<Long, ProductSkuDO> existingSkuById, Map<String, ProductSkuDO> activeSkuByCode) {
        Map<String, Long> incomingSkuCodeCount = productSpu.skus().stream().collect(Collectors.groupingBy(ProductSku::skuCode, LinkedHashMap::new, Collectors.counting()));
        List<String> duplicatedSkuCodes = incomingSkuCodeCount.entrySet().stream().filter(entry -> entry.getValue() > 1).map(Map.Entry::getKey).toList();
        if (!duplicatedSkuCodes.isEmpty()) throw BusinessException.badRequest("SKU编码重复: " + String.join(", ", duplicatedSkuCodes));
        for (ProductSku sku : productSpu.skus()) {
            if (sku.id() != null) {
                ProductSkuDO existingSku = existingSkuById.get(sku.id());
                if (existingSku == null) throw BusinessException.badRequest("SKU不存在或不属于当前商品: " + sku.id());
                ProductSkuDO activeSku = activeSkuByCode.get(sku.skuCode());
                if (activeSku != null && !activeSku.getId().equals(sku.id())) throw BusinessException.badRequest("SKU编码已存在: " + sku.skuCode());
                continue;
            }
            if (activeSkuByCode.containsKey(sku.skuCode())) throw BusinessException.badRequest("SKU编码已存在: " + sku.skuCode());
        }
    }

    private ProductSpuDO toSpuDO(ProductSpu productSpu) {
        ProductSpuDO spuDO = new ProductSpuDO();
        spuDO.setName(productSpu.name()); spuDO.setCategoryId(productSpu.categoryId()); spuDO.setMainImageUrl(productSpu.mainImageUrl()); spuDO.setAlbumImagesJson(productSpu.albumImagesJson()); spuDO.setDescription(productSpu.description()); spuDO.setStatus(productSpu.status());
        return spuDO;
    }

    private ProductSpu toDomain(ProductSpuDO spuDO) {
        List<ProductSku> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuDO>().eq(ProductSkuDO::getSpuId, spuDO.getId()).isNull(ProductSkuDO::getDeletedAt).orderByAsc(ProductSkuDO::getId)).stream().map(this::toDomain).toList();
        return new ProductSpu(spuDO.getId(), spuDO.getName(), spuDO.getCategoryId(), spuDO.getMainImageUrl(), spuDO.getAlbumImagesJson(), spuDO.getDescription(), spuDO.getStatus(), skus);
    }

    private ProductSku toDomain(ProductSkuDO skuDO) {
        return new ProductSku(skuDO.getId(), skuDO.getSpuId(), skuDO.getSkuCode(), skuDO.getSkuName(), skuDO.getSpecJson(), skuDO.getSalePriceCent(), skuDO.getOriginPriceCent(), skuDO.getSalesCount(), skuDO.getStatus());
    }
}
