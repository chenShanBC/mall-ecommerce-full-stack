package com.mallfei.product.domain.service;

import com.mallfei.common.exception.BusinessException;
import com.mallfei.product.application.dto.AdminCreateProductRequest;
import com.mallfei.product.application.dto.AdminUpdateProductRequest;
import com.mallfei.product.domain.model.Category;
import com.mallfei.product.domain.model.ProductSku;
import com.mallfei.product.domain.model.ProductSpu;
import com.mallfei.product.domain.repository.CategoryRepository;
import com.mallfei.product.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductDomainService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public ProductDomainService(CategoryRepository categoryRepository,
                                ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public List<Category> loadEnabledCategories() {
        return categoryRepository.findAllEnabled();
    }

    public List<Category> loadAllCategories() {
        return categoryRepository.findAll();
    }

    public Category loadCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> BusinessException.badRequest("类目不存在"));
    }

    public Category createCategory(String name, Long parentId, Integer sortOrder) {
        validateCategoryName(name);
        validateCategoryNameUnique(null, name);
        validateCategoryParent(null, parentId);
        return categoryRepository.save(Category.create(name.trim(), parentId, sortOrder));
    }

    public Category updateCategory(Long categoryId, String name, Long parentId, Integer sortOrder, String status) {
        Category existing = loadCategory(categoryId);
        validateCategoryName(name);
        validateCategoryNameUnique(categoryId, name);
        validateCategoryParent(categoryId, parentId);
        return categoryRepository.update(existing.applyUpdate(name.trim(), parentId, sortOrder, status));
    }

    public Category updateCategoryStatus(Long categoryId, String status) {
        Category existing = loadCategory(categoryId);
        return categoryRepository.update(existing.applyStatus(status));
    }

    public void deleteCategory(Long categoryId) {
        loadCategory(categoryId);
        boolean hasChildren = categoryRepository.findAll().stream().anyMatch(category -> categoryId.equals(category.parentId()));
        if (hasChildren) {
            throw BusinessException.badRequest("该分类下存在子分类，不能删除");
        }
        if (productRepository.countByCategoryId(categoryId) > 0) {
            throw BusinessException.badRequest("该分类下存在商品，不能删除");
        }
        categoryRepository.softDelete(categoryId);
    }

    public List<ProductSpu> loadOnlineProducts() {
        return productRepository.findAllOnline();
    }

    public List<ProductSpu> loadAllProducts() {
        return productRepository.findAll();
    }

    public ProductSpu loadOnlineProduct(Long productId) {
        return productRepository.findOnlineById(productId)
                .orElseThrow(() -> BusinessException.badRequest("商品不存在或已下架"));
    }

    public ProductSpu loadProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> BusinessException.badRequest("商品不存在"));
    }

    public ProductSpu createProduct(AdminCreateProductRequest request) {
        validateInitialStock(request.skus().stream().map(AdminCreateProductRequest.SkuInput::initialStock).toList());
        return productRepository.save(buildProductSpu(null, request.name(), request.categoryId(), request.mainImageUrl(), request.description(), "ONLINE", request.skus().stream().map(this::toSku).toList()));
    }

    public ProductSpu updateProduct(Long productId, AdminUpdateProductRequest request) {
        ProductSpu existing = loadProduct(productId);
        validateInitialStock(request.skus().stream().map(AdminUpdateProductRequest.SkuInput::initialStock).toList());
        if ("ONLINE".equalsIgnoreCase(request.status())) {
            boolean hasNewSku = request.skus().stream().anyMatch(sku -> sku.id() == null);
            if (hasNewSku) {
                throw BusinessException.badRequest("商品上架状态不允许新增SKU及初始库存，请先保存为下架状态或通过库存模块处理");
            }
        }
        return productRepository.update(existing.applyUpdate(
                request.name(),
                request.categoryId(),
                request.mainImageUrl(),
                request.description(),
                request.status(),
                request.skus().stream().map(sku -> toSku(sku, existing)).toList()
        ));
    }

    public ProductSpu updateProductStatus(Long productId, String status) {
        ProductSpu existing = loadProduct(productId);
        ProductSpu candidate = existing.applyStatus(status);
        productRepository.updateStatus(productId, candidate.status());
        return loadProduct(productId);
    }

    public void incrementSkuSales(List<ProductRepository.SkuSalesIncrement> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        productRepository.incrementSkuSales(items);
    }

    public void decrementSkuSales(List<ProductRepository.SkuSalesIncrement> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        productRepository.decrementSkuSales(items);
    }

    private ProductSpu buildProductSpu(Long id, String name, Long categoryId, String mainImageUrl, String description, String status, List<ProductSku> skus) {
        return ProductSpu.create(id, name, categoryId, mainImageUrl, "[]", description, status, skus);
    }

    private void validateCategoryName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw BusinessException.badRequest("类目名称不能为空");
        }
        if (name.trim().length() > 64) {
            throw BusinessException.badRequest("类目名称不能超过64个字符");
        }
    }

    private void validateCategoryNameUnique(Long categoryId, String name) {
        String normalizedName = name == null ? "" : name.trim();
        boolean duplicated = categoryRepository.findAll().stream()
                .anyMatch(category -> normalizedName.equals(category.name()) && !category.id().equals(categoryId));
        if (duplicated) {
            throw BusinessException.badRequest("分类名称已存在，请使用其他名称");
        }
    }

    private void validateCategoryParent(Long categoryId, Long parentId) {
        Long normalizedParentId = parentId == null ? 0L : parentId;
        if (normalizedParentId <= 0) {
            return;
        }
        if (normalizedParentId.equals(categoryId)) {
            throw BusinessException.badRequest("父类目不能选择自身");
        }
        Category parent = loadCategory(normalizedParentId);
        if (parent.level() != null && parent.level() >= 2) {
            throw BusinessException.badRequest("当前仅支持二级类目");
        }
    }

    private void validateInitialStock(List<Integer> initialStocks) {
        if (initialStocks.stream().anyMatch(stock -> stock == null || stock < 0)) {
            throw BusinessException.badRequest("初始库存不能小于0");
        }
    }

    private ProductSku toSku(AdminCreateProductRequest.SkuInput skuInput) {
        return new ProductSku(null, null, skuInput.skuCode(), skuInput.skuName(), skuInput.specJson() == null ? "{}" : skuInput.specJson(), skuInput.salePriceCent(), skuInput.originPriceCent(), 0, "ONLINE");
    }

    private ProductSku toSku(AdminUpdateProductRequest.SkuInput skuInput, ProductSpu existing) {
        int currentSalesCount = existing.skus().stream()
                .filter(sku -> skuInput.id() != null && skuInput.id().equals(sku.id()))
                .map(ProductSku::salesCount)
                .findFirst()
                .orElse(0);
        return new ProductSku(skuInput.id(), null, skuInput.skuCode(), skuInput.skuName(), skuInput.specJson() == null ? "{}" : skuInput.specJson(), skuInput.salePriceCent(), skuInput.originPriceCent(), currentSalesCount, skuInput.status());
    }
}
