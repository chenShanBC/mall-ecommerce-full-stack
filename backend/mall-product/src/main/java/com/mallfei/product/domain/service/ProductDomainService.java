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
        return categoryRepository.save(Category.create(name, parentId, sortOrder));
    }

    public Category updateCategory(Long categoryId, String name, Long parentId, Integer sortOrder, String status) {
        Category existing = loadCategory(categoryId);
        return categoryRepository.update(existing.applyUpdate(name, parentId, sortOrder, status));
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
        if (existing.online()) {
            boolean hasNewSku = request.skus().stream().anyMatch(sku -> sku.id() == null);
            if (hasNewSku) {
                throw BusinessException.badRequest("商品已上架，新增SKU及初始库存请先下架或通过库存模块处理");
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
