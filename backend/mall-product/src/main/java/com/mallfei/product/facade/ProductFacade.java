package com.mallfei.product.facade;

import com.mallfei.common.api.PageResponse;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.product.application.dto.AdminCreateProductRequest;
import com.mallfei.product.application.dto.AdminProductPageQuery;
import com.mallfei.product.application.dto.AdminUpdateProductRequest;
import com.mallfei.product.application.service.ProductCommandApplicationService;
import com.mallfei.product.application.service.ProductQueryApplicationService;
import com.mallfei.product.application.service.ProductSalesStatApplicationService;
import com.mallfei.product.application.vo.AdminProductDetailView;
import com.mallfei.product.application.vo.AdminProductPageRowView;
import com.mallfei.product.application.vo.AdminProductSummaryView;
import com.mallfei.product.application.vo.CategoryAdminView;
import com.mallfei.product.domain.model.Category;
import com.mallfei.product.domain.model.ProductSku;
import com.mallfei.product.domain.model.ProductSpu;
import com.mallfei.product.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class ProductFacade {

    private final ProductQueryApplicationService productQueryApplicationService;
    private final ProductCommandApplicationService productCommandApplicationService;
    private final ProductSalesStatApplicationService productSalesStatApplicationService;
    private final ProductRepository productRepository;

    public ProductFacade(ProductQueryApplicationService productQueryApplicationService,
                         ProductCommandApplicationService productCommandApplicationService,
                         ProductSalesStatApplicationService productSalesStatApplicationService,
                         ProductRepository productRepository) {
        this.productQueryApplicationService = productQueryApplicationService;
        this.productCommandApplicationService = productCommandApplicationService;
        this.productSalesStatApplicationService = productSalesStatApplicationService;
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
    public List<ProductSpu> findAll() { return productRepository.findAll(); }

    public ProductSkuSnapshot getSkuSnapshot(Long skuId) {
        ProductSpu product = productRepository.findBySkuId(skuId).orElseThrow(() -> BusinessException.badRequest("商品SKU不存在: " + skuId));
        ProductSku sku = product.skus().stream().filter(item -> item.id().equals(skuId)).findFirst().orElseThrow(() -> BusinessException.badRequest("商品SKU不存在: " + skuId));
        return new ProductSkuSnapshot(sku.id(), product.id(), product.name(), product.status(), product.mainImageUrl(), sku.skuName(), sku.skuCode(), sku.specJson(), sku.salePriceCent(), sku.originPriceCent(), sku.status());
    }

    public void incrementSkuSales(List<ProductRepository.SkuSalesIncrement> items) { productCommandApplicationService.incrementSkuSales(items); }
    public void decrementSkuSales(List<ProductRepository.SkuSalesIncrement> items) { productCommandApplicationService.decrementSkuSales(items); }
    public boolean recordOrderCompletedSales(String orderNo, LocalDateTime completedAt, List<ProductSalesItem> items) {
        return productSalesStatApplicationService.recordOrderCompleted(orderNo, completedAt, items.stream()
                .map(item -> new ProductSalesStatApplicationService.OrderCompletedSalesItem(item.spuId(), item.skuId(), item.quantity(), item.amountCent()))
                .toList());
    }
    public Map<Long, ProductSalesStatApplicationService.ProductSalesAggregate> recent30DaySalesBySpuIds(List<Long> spuIds) { return productSalesStatApplicationService.recent30DaySalesBySpuIds(spuIds); }
    public Map<Long, ProductSalesStatApplicationService.ProductSalesAggregate> recent30DaySalesBySpu() { return productSalesStatApplicationService.recent30DaySalesBySpu(); }
    public long recent7DaySalesCount() { return productSalesStatApplicationService.recent7DaySalesCount(); }
    public long recent30DaySalesCount() { return productSalesStatApplicationService.recent30DaySalesCount(); }
    public long currentMonthSalesCount() { return productSalesStatApplicationService.currentMonthSalesCount(); }
    public long recent30DaySalesAmountCent() { return productSalesStatApplicationService.recent30DaySalesAmountCent(); }
    public List<ProductSalesStatApplicationService.ProductSalesMonthlyAggregate> recentMonthlySalesTrend(int monthCount) { return productSalesStatApplicationService.recentMonthlySalesTrend(monthCount); }

    public record ProductSalesItem(Long spuId, Long skuId, Integer quantity, Long amountCent) {}
}
