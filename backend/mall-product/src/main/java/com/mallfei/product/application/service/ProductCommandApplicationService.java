package com.mallfei.product.application.service;

import com.mallfei.product.application.assembler.ProductViewAssembler;
import com.mallfei.product.application.dto.AdminCreateProductRequest;
import com.mallfei.product.application.dto.AdminUpdateProductRequest;
import com.mallfei.product.application.vo.AdminProductSummaryView;
import com.mallfei.product.domain.model.Category;
import com.mallfei.product.domain.model.ProductSku;
import com.mallfei.product.domain.model.ProductSpu;
import com.mallfei.product.domain.repository.ProductRepository;
import com.mallfei.product.domain.service.ProductDomainService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductCommandApplicationService {

    private final ProductDomainService productDomainService;
    private final ProductViewAssembler productViewAssembler;
    private final ProductStockInitEventPublisher productStockInitEventPublisher;

    public ProductCommandApplicationService(ProductDomainService productDomainService,
                                            ProductViewAssembler productViewAssembler,
                                            ProductStockInitEventPublisher productStockInitEventPublisher) {
        this.productDomainService = productDomainService;
        this.productViewAssembler = productViewAssembler;
        this.productStockInitEventPublisher = productStockInitEventPublisher;
    }

    public Category createCategory(String name, Long parentId, Integer sortOrder) {
        return productDomainService.createCategory(name, parentId, sortOrder);
    }

    public Category updateCategory(Long categoryId, String name, Long parentId, Integer sortOrder, String status) {
        return productDomainService.updateCategory(categoryId, name, parentId, sortOrder, status);
    }

    public Category updateCategoryStatus(Long categoryId, String status) {
        return productDomainService.updateCategoryStatus(categoryId, status);
    }

    public void deleteCategory(Long categoryId) {
        productDomainService.deleteCategory(categoryId);
    }

    public AdminProductSummaryView createProduct(AdminCreateProductRequest request) {
        ProductSpu persisted = productDomainService.createProduct(request);
        publishStockInit(persisted.skus(), request.skus().stream().collect(Collectors.toMap(AdminCreateProductRequest.SkuInput::skuCode, AdminCreateProductRequest.SkuInput::initialStock, (left, right) -> right, LinkedHashMap::new)));
        return productViewAssembler.toAdminProductSummary(persisted);
    }

    public AdminProductSummaryView updateProduct(Long productId, AdminUpdateProductRequest request) {
        ProductSpu updated = productDomainService.updateProduct(productId, request);
        publishStockInit(updated.skus(), request.skus().stream().collect(Collectors.toMap(AdminUpdateProductRequest.SkuInput::skuCode, AdminUpdateProductRequest.SkuInput::initialStock, (left, right) -> right, LinkedHashMap::new)));
        return productViewAssembler.toAdminProductSummary(updated);
    }

    public AdminProductSummaryView updateProductStatus(Long productId, String status) {
        ProductSpu updated = productDomainService.updateProductStatus(productId, status);
        if (updated.online()) {
            publishStockInit(updated.skus(), Map.of());
        }
        return productViewAssembler.toAdminProductSummary(updated);
    }

    public void incrementSkuSales(List<ProductRepository.SkuSalesIncrement> items) {
        productDomainService.incrementSkuSales(items);
    }

    public void decrementSkuSales(List<ProductRepository.SkuSalesIncrement> items) {
        productDomainService.decrementSkuSales(items);
    }

    private void publishStockInit(List<ProductSku> skus, Map<String, Integer> initialStockBySkuCode) {
        if (skus == null) {
            return;
        }
        var items = skus.stream()
                .filter(sku -> sku.id() != null)
                .map(sku -> new com.mallfei.common.messaging.ProductStockInitEvent.SkuStockInitItem(sku.id(), initialStockBySkuCode.getOrDefault(sku.skuCode(), 100)))
                .toList();
        if (items.isEmpty()) {
            return;
        }
        productStockInitEventPublisher.publish(new com.mallfei.common.messaging.ProductStockInitEvent(items));
    }
}
