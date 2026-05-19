package com.mallfei.product.application.service;

import com.mallfei.common.api.PageResponse;
import com.mallfei.common.api.PageResult;
import com.mallfei.product.application.assembler.ProductViewAssembler;
import com.mallfei.product.application.dto.AdminProductPageQuery;
import com.mallfei.product.application.vo.AdminProductDetailView;
import com.mallfei.product.application.vo.AdminProductPageRowView;
import com.mallfei.product.application.vo.CategoryAdminView;
import com.mallfei.product.application.vo.CategoryTreeNodeView;
import com.mallfei.product.application.vo.ProductCardView;
import com.mallfei.product.application.vo.ProductDetailView;
import com.mallfei.product.domain.model.ProductSpu;
import com.mallfei.product.domain.service.ProductDomainService;
import com.mallfei.stock.facade.StockFacade;
import com.mallfei.stock.facade.StockSnapshot;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductQueryApplicationService {

    private final ProductDomainService productDomainService;
    private final ProductViewAssembler productViewAssembler;
    private final StockFacade stockFacade;

    public ProductQueryApplicationService(ProductDomainService productDomainService,
                                          ProductViewAssembler productViewAssembler,
                                          StockFacade stockFacade) {
        this.productDomainService = productDomainService;
        this.productViewAssembler = productViewAssembler;
        this.stockFacade = stockFacade;
    }

    public List<CategoryTreeNodeView> categories() {
        return productViewAssembler.toCategoryTree(productDomainService.loadEnabledCategories());
    }

    public PageResponse<ProductCardView> productPage() {
        List<ProductSpu> onlineProducts = productDomainService.loadOnlineProducts().stream()
                .filter(productSpu -> !productSpu.skus().isEmpty())
                .toList();
        Map<Long, ProductSpu> productBySkuId = onlineProducts.stream()
                .flatMap(product -> product.skus().stream().filter(sku -> sku.id() != null).map(sku -> Map.entry(sku.id(), product)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));
        Map<Long, Integer> availableStockByProductId = stockFacade.stockListBySkuIds(productBySkuId.keySet().stream().toList()).stream()
                .filter(stock -> stock.availableStock() > 0)
                .collect(Collectors.groupingBy(stock -> productBySkuId.get(stock.skuId()).id(), LinkedHashMap::new, Collectors.summingInt(StockSnapshot::availableStock)));
        List<ProductCardView> records = onlineProducts.stream()
                .filter(product -> availableStockByProductId.getOrDefault(product.id(), 0) > 0)
                .map(productViewAssembler::toProductCard)
                .toList();
        return new PageResponse<>(records, records.size(), 1, 10);
    }

    public ProductDetailView productDetail(Long productId) {
        ProductSpu product = productDomainService.loadOnlineProduct(productId);
        List<Long> skuIds = product.skus().stream().map(sku -> sku.id()).filter(id -> id != null).toList();
        Map<Long, StockSnapshot> stockBySkuId = stockFacade.stockListBySkuIds(skuIds).stream()
                .collect(Collectors.toMap(StockSnapshot::skuId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        return productViewAssembler.toProductDetail(product, stockBySkuId);
    }

    public List<CategoryAdminView> adminCategories() {
        return productDomainService.loadAllCategories().stream()
                .map(productViewAssembler::toCategoryAdminView)
                .toList();
    }

    public PageResponse<AdminProductPageRowView> adminProductPage(AdminProductPageQuery query) {
        List<ProductSpu> filtered = productDomainService.loadAllProducts().stream()
                .filter(product -> query.keyword() == null || query.keyword().isBlank() || product.name().contains(query.keyword()))
                .filter(product -> query.categoryId() == null || query.categoryId().equals(product.categoryId()))
                .filter(product -> query.status() == null || query.status().isBlank() || query.status().equals(product.status()))
                .sorted(productComparator(query.sortBy(), query.sortOrder()))
                .toList();

        PageResult<ProductSpu> pageResult = PageResult.of(filtered, query.page(), query.size());
        List<ProductSpu> pageProducts = pageResult.records();
        Map<Long, ProductSpu> productBySkuId = pageProducts.stream()
                .flatMap(product -> product.skus().stream().filter(sku -> sku.id() != null).map(sku -> Map.entry(sku.id(), product)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));
        Map<Long, Integer> stockTotalByProductId = stockFacade.stockListBySkuIds(productBySkuId.keySet().stream().toList()).stream()
                .collect(Collectors.groupingBy(stock -> productBySkuId.get(stock.skuId()).id(), LinkedHashMap::new, Collectors.summingInt(StockSnapshot::totalStock)));
        List<AdminProductPageRowView> rows = pageProducts.stream()
                .map(product -> productViewAssembler.toAdminProductPageRow(product, stockTotalByProductId.getOrDefault(product.id(), 0)))
                .toList();
        return new PageResponse<>(rows, pageResult.total(), pageResult.page(), pageResult.size());
    }

    public AdminProductDetailView adminProductDetail(Long productId) {
        var product = productDomainService.loadProduct(productId);
        List<Long> skuIds = product.skus().stream().map(sku -> sku.id()).filter(id -> id != null).toList();
        Map<Long, StockSnapshot> stockBySkuId = stockFacade.stockListBySkuIds(skuIds).stream()
                .collect(Collectors.toMap(StockSnapshot::skuId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        return productViewAssembler.toAdminProductDetail(product, stockBySkuId);
    }

    private Comparator<ProductSpu> productComparator(String sortBy, String sortOrder) {
        Comparator<ProductSpu> comparator = switch (blank(sortBy) ? "id" : sortBy) {
            case "id" -> Comparator.comparing(ProductSpu::id, Comparator.nullsLast(Long::compareTo));
            case "name" -> Comparator.comparing(ProductSpu::name, Comparator.nullsLast(String::compareTo));
            case "categoryId" -> Comparator.comparing(ProductSpu::categoryId, Comparator.nullsLast(Long::compareTo));
            case "status" -> Comparator.comparing(ProductSpu::status, Comparator.nullsLast(String::compareTo));
            case "skuCount" -> Comparator.comparing(product -> product.skus() == null ? 0 : product.skus().size(), Comparator.nullsLast(Integer::compareTo));
            default -> Comparator.comparing(ProductSpu::id, Comparator.nullsLast(Long::compareTo));
        };
        return "desc".equalsIgnoreCase(sortOrder) ? comparator.reversed() : comparator;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
