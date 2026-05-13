package com.mallfei.product.facade;

import com.mallfei.common.api.PageResponse;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.product.application.dto.AdminCreateProductRequest;
import com.mallfei.product.application.dto.AdminProductPageQuery;
import com.mallfei.product.application.dto.AdminUpdateProductRequest;
import com.mallfei.product.application.service.ProductCommandApplicationService;
import com.mallfei.product.application.service.ProductQueryApplicationService;
import com.mallfei.product.application.vo.AdminProductDetailView;
import com.mallfei.product.application.vo.AdminProductPageRowView;
import com.mallfei.product.application.vo.AdminProductSummaryView;
import com.mallfei.product.application.vo.CategoryAdminView;
import com.mallfei.product.domain.model.Category;
import com.mallfei.product.domain.model.ProductSku;
import com.mallfei.product.domain.model.ProductSpu;
import com.mallfei.product.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductFacade {

    private final ProductQueryApplicationService productQueryApplicationService;
    private final ProductCommandApplicationService productCommandApplicationService;
    private final ProductRepository productRepository;

    public ProductFacade(ProductQueryApplicationService productQueryApplicationService,
                         ProductCommandApplicationService productCommandApplicationService,
                         ProductRepository productRepository) {
        this.productQueryApplicationService = productQueryApplicationService;
        this.productCommandApplicationService = productCommandApplicationService;
        this.productRepository = productRepository;
    }

    public List<CategoryAdminView> adminCategories() { return productQueryApplicationService.adminCategories(); }
    public Category createCategory(String name, Long parentId, Integer sortOrder) { return productCommandApplicationService.createCategory(name, parentId, sortOrder); }
    public Category updateCategory(Long categoryId, String name, Long parentId, Integer sortOrder, String status) { return productCommandApplicationService.updateCategory(categoryId, name, parentId, sortOrder, status); }
    public PageResponse<AdminProductPageRowView> adminProductPage(AdminProductPageQuery query) { return productQueryApplicationService.adminProductPage(query); }
    public AdminProductDetailView adminProductDetail(Long productId) { return productQueryApplicationService.adminProductDetail(productId); }
    public AdminProductSummaryView createProduct(AdminCreateProductRequest request) { return productCommandApplicationService.createProduct(request); }
    public AdminProductSummaryView updateProduct(Long productId, AdminUpdateProductRequest request) { return productCommandApplicationService.updateProduct(productId, request); }
    public AdminProductSummaryView updateProductStatus(Long productId, String status) { return productCommandApplicationService.updateProductStatus(productId, status); }

    public ProductSkuSnapshot getSkuSnapshot(Long skuId) {
        ProductSpu product = productRepository.findBySkuId(skuId).orElseThrow(() -> BusinessException.badRequest("商品SKU不存在: " + skuId));
        ProductSku sku = product.skus().stream().filter(item -> item.id().equals(skuId)).findFirst().orElseThrow(() -> BusinessException.badRequest("商品SKU不存在: " + skuId));
        return new ProductSkuSnapshot(sku.id(), product.id(), product.name(), product.status(), product.mainImageUrl(), sku.skuName(), sku.skuCode(), sku.specJson(), sku.salePriceCent(), sku.originPriceCent(), sku.status());
    }

    public void incrementSkuSales(List<ProductRepository.SkuSalesIncrement> items) { productCommandApplicationService.incrementSkuSales(items); }
    public void decrementSkuSales(List<ProductRepository.SkuSalesIncrement> items) { productCommandApplicationService.decrementSkuSales(items); }
}
