package com.mallfei.product.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallfei.product.domain.model.Category;
import com.mallfei.product.domain.repository.CategoryRepository;
import com.mallfei.product.infrastructure.persistence.dataobject.CategoryDO;
import com.mallfei.product.infrastructure.persistence.mapper.CategoryMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisCategoryRepository implements CategoryRepository {

    private final CategoryMapper categoryMapper;

    public MybatisCategoryRepository(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<Category> findAllEnabled() {
        return categoryMapper.selectList(new LambdaQueryWrapper<CategoryDO>()
                        .eq(CategoryDO::getStatus, "ENABLED")
                        .isNull(CategoryDO::getDeletedAt)
                        .orderByAsc(CategoryDO::getSortOrder)
                        .orderByAsc(CategoryDO::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Category> findAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<CategoryDO>()
                        .isNull(CategoryDO::getDeletedAt)
                        .orderByAsc(CategoryDO::getSortOrder)
                        .orderByAsc(CategoryDO::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Category> findById(Long id) {
        CategoryDO categoryDO = categoryMapper.selectOne(new LambdaQueryWrapper<CategoryDO>()
                .eq(CategoryDO::getId, id)
                .isNull(CategoryDO::getDeletedAt)
                .last("limit 1"));
        return Optional.ofNullable(categoryDO).map(this::toDomain);
    }

    @Override
    public Category save(Category category) {
        CategoryDO categoryDO = new CategoryDO();
        categoryDO.setName(category.name());
        categoryDO.setParentId(category.parentId());
        categoryDO.setLevel(category.level());
        categoryDO.setSortOrder(category.sortOrder());
        categoryDO.setStatus(category.status());
        categoryMapper.insert(categoryDO);
        return toDomain(categoryDO);
    }

    @Override
    public Category update(Category category) {
        CategoryDO categoryDO = new CategoryDO();
        categoryDO.setId(category.id());
        categoryDO.setName(category.name());
        categoryDO.setParentId(category.parentId());
        categoryDO.setLevel(category.level());
        categoryDO.setSortOrder(category.sortOrder());
        categoryDO.setStatus(category.status());
        categoryMapper.updateById(categoryDO);
        return findById(category.id()).orElseThrow();
    }

    @Override
    public void softDelete(Long id) {
        CategoryDO categoryDO = new CategoryDO();
        categoryDO.setId(id);
        categoryDO.setDeletedAt(java.time.LocalDateTime.now());
        categoryMapper.updateById(categoryDO);
    }

    private Category toDomain(CategoryDO categoryDO) {
        return new Category(
                categoryDO.getId(),
                categoryDO.getName(),
                categoryDO.getParentId(),
                categoryDO.getLevel(),
                categoryDO.getSortOrder(),
                categoryDO.getStatus()
        );
    }
}
