package com.mallfei.product.application.service;

import com.mallfei.common.api.PageResponse;
import com.mallfei.common.messaging.ProductStockInitEvent;
import com.mallfei.product.application.assembler.ProductViewAssembler;
import com.mallfei.product.application.dto.AdminCreateProductRequest;
import com.mallfei.product.application.dto.AdminProductPageQuery;
import com.mallfei.product.application.dto.AdminUpdateProductRequest;
import com.mallfei.product.application.vo.AdminProductDetailView;
import com.mallfei.product.application.vo.AdminProductPageRowView;
import com.mallfei.product.application.vo.AdminProductSummaryView;
import com.mallfei.product.application.vo.CategoryAdminView;
import com.mallfei.product.application.vo.CategoryTreeNodeView;
import com.mallfei.product.application.vo.ProductCardView;
import com.mallfei.product.application.vo.ProductDetailView;
import com.mallfei.product.domain.model.Category;
import com.mallfei.product.domain.model.ProductSku;
import com.mallfei.product.domain.model.ProductSpu;
import com.mallfei.product.domain.repository.ProductRepository;
import com.mallfei.product.domain.service.ProductDomainService;
import com.mallfei.stock.facade.StockFacade;
import com.mallfei.stock.facade.StockSnapshot;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductApplicationService {

    private final ProductDomainService productDomainService;
    private final ProductViewAssembler productViewAssembler;
    private final StockFacade stockFacade;
    private final ProductSalesStatApplicationService productSalesStatApplicationService;
    private final ProductStockInitEventPublisher productStockInitEventPublisher;

    public ProductApplicationService(ProductDomainService productDomainService,
                                     ProductViewAssembler productViewAssembler,
                                     StockFacade stockFacade,
                                     ProductSalesStatApplicationService productSalesStatApplicationService,
                                     ProductStockInitEventPublisher productStockInitEventPublisher) {
        this.productDomainService = productDomainService;
        this.productViewAssembler = productViewAssembler;
        this.stockFacade = stockFacade;
        this.productSalesStatApplicationService = productSalesStatApplicationService;
        this.productStockInitEventPublisher = productStockInitEventPublisher;
    }

    public List<CategoryTreeNodeView> categories() { return productViewAssembler.toCategoryTree(productDomainService.loadEnabledCategories()); }
    public PageResponse<ProductCardView> productPage() { List<ProductCardView> records = productDomainService.loadOnlineProducts().stream().filter(productSpu -> !productSpu.skus().isEmpty()).map(productViewAssembler::toProductCard).toList(); return new PageResponse<>(records, records.size(), 1, 10); }
    public ProductDetailView productDetail(Long productId) { return productViewAssembler.toProductDetail(productDomainService.loadOnlineProduct(productId)); }
    public List<CategoryAdminView> adminCategories() { return productDomainService.loadAllCategories().stream().map(productViewAssembler::toCategoryAdminView).toList(); }
    public Category createCategory(String name, Long parentId, Integer sortOrder) { return productDomainService.createCategory(name, parentId, sortOrder); }
    public Category updateCategory(Long categoryId, String name, Long parentId, Integer sortOrder, String status) { return productDomainService.updateCategory(categoryId, name, parentId, sortOrder, status); }
    public PageResponse<AdminProductPageRowView> adminProductPage() { return new ProductQueryApplicationService(productDomainService, productViewAssembler, stockFacade, productSalesStatApplicationService).adminProductPage(new AdminProductPageQuery(null, null, null, null, null, null, null, null, null, 1, 10)); }

    public AdminProductDetailView adminProductDetail(Long productId) {
        ProductSpu product = productDomainService.loadProduct(productId);
        List<Long> skuIds = product.skus().stream().map(ProductSku::id).filter(id -> id != null).toList();
        Map<Long, StockSnapshot> stockBySkuId = stockFacade.stockListBySkuIds(skuIds).stream().collect(Collectors.toMap(StockSnapshot::skuId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        return productViewAssembler.toAdminProductDetail(product, stockBySkuId);
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
        if (updated.online()) { publishStockInit(updated.skus(), Map.of()); }
        return productViewAssembler.toAdminProductSummary(updated);
    }

    public void incrementSkuSales(List<ProductRepository.SkuSalesIncrement> items) { productDomainService.incrementSkuSales(items); }
    public void decrementSkuSales(List<ProductRepository.SkuSalesIncrement> items) { productDomainService.decrementSkuSales(items); }

    private void publishStockInit(List<ProductSku> skus, Map<String, Integer> initialStockBySkuCode) {
        if (skus == null) return;
        List<ProductStockInitEvent.SkuStockInitItem> items = skus.stream().filter(sku -> sku.id() != null).map(sku -> new ProductStockInitEvent.SkuStockInitItem(sku.id(), initialStockBySkuCode.getOrDefault(sku.skuCode(), 100))).toList();
        if (items.isEmpty()) return;
        productStockInitEventPublisher.publish(new ProductStockInitEvent(items));
    }
}
