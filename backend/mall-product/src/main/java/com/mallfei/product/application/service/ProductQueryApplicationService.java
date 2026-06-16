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

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductQueryApplicationService {

    private static final Duration PUBLIC_CACHE_TTL = Duration.ofSeconds(30);
    private static final int DEFAULT_HOT_SALES_THRESHOLD = 100;
    private static final int DEFAULT_LOW_SALES_THRESHOLD = 10;

    private final ProductDomainService productDomainService;
    private final ProductViewAssembler productViewAssembler;
    private final StockFacade stockFacade;
    private final ProductSalesStatApplicationService productSalesStatApplicationService;
    private volatile CacheEntry<List<CategoryTreeNodeView>> categoriesCache;
    private volatile CacheEntry<PageResponse<ProductCardView>> productPageCache;

    public ProductQueryApplicationService(ProductDomainService productDomainService,
                                          ProductViewAssembler productViewAssembler,
                                          StockFacade stockFacade,
                                          ProductSalesStatApplicationService productSalesStatApplicationService) {
        this.productDomainService = productDomainService;
        this.productViewAssembler = productViewAssembler;
        this.stockFacade = stockFacade;
        this.productSalesStatApplicationService = productSalesStatApplicationService;
    }

    public List<CategoryTreeNodeView> categories() {
        CacheEntry<List<CategoryTreeNodeView>> cached = categoriesCache;
        if (cached != null && !cached.expired()) {
            return cached.value();
        }
        List<CategoryTreeNodeView> result = productViewAssembler.toCategoryTree(productDomainService.loadEnabledCategories());
        categoriesCache = CacheEntry.of(result, PUBLIC_CACHE_TTL);
        return result;
    }

    public PageResponse<ProductCardView> productPage() {
        return productPage(1, 10);
    }

    public PageResponse<ProductCardView> productPage(long page, long size) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), 50);
        CacheEntry<PageResponse<ProductCardView>> cached = productPageCache;
        if (cached != null && !cached.expired()) {
            return sliceProductPage(cached.value().records(), safePage, safeSize);
        }
        List<ProductSpu> onlineProducts = productDomainService.loadOnlineProducts().stream()
                .filter(productSpu -> !productSpu.skus().isEmpty())
                .toList();
        Map<Long, ProductSpu> productBySkuId = onlineProducts.stream()
                .flatMap(product -> product.skus().stream()
                        .filter(sku -> sku.id() != null && sku.enabled())
                        .map(sku -> Map.entry(sku.id(), product)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));
        Map<Long, Integer> availableStockByProductId = stockFacade.stockListBySkuIds(productBySkuId.keySet().stream().toList()).stream()
                .filter(stock -> stock.availableStock() > 0)
                .collect(Collectors.groupingBy(stock -> productBySkuId.get(stock.skuId()).id(), LinkedHashMap::new, Collectors.summingInt(StockSnapshot::availableStock)));
        List<ProductCardView> records = onlineProducts.stream()
                .filter(product -> availableStockByProductId.getOrDefault(product.id(), 0) > 0)
                .map(productViewAssembler::toProductCard)
                .toList();
        PageResponse<ProductCardView> fullResult = new PageResponse<>(records, records.size(), 1, records.size());
        productPageCache = CacheEntry.of(fullResult, PUBLIC_CACHE_TTL);
        return sliceProductPage(records, safePage, safeSize);
    }

    private PageResponse<ProductCardView> sliceProductPage(List<ProductCardView> records, long page, long size) {
        int fromIndex = (int) Math.min((page - 1) * size, records.size());
        int toIndex = (int) Math.min(fromIndex + size, records.size());
        return new PageResponse<>(records.subList(fromIndex, toIndex), records.size(), page, size);
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
        int hotSalesThreshold = positiveOrDefault(query.hotSalesThreshold(), DEFAULT_HOT_SALES_THRESHOLD);
        int lowSalesThreshold = positiveOrDefault(query.lowSalesThreshold(), DEFAULT_LOW_SALES_THRESHOLD);
        List<ProductSpu> allProducts = productDomainService.loadAllProducts();
        Map<Long, ProductSalesStatApplicationService.ProductSalesAggregate> recent30DaySalesBySpu = productSalesStatApplicationService.recent30DaySalesBySpuIds(allProducts.stream().map(ProductSpu::id).toList());
        List<ProductSpu> filtered = allProducts.stream()
                .filter(product -> query.keyword() == null || query.keyword().isBlank() || product.name().contains(query.keyword()))
                .filter(product -> query.categoryId() == null || query.categoryId().equals(product.categoryId()))
                .filter(product -> query.status() == null || query.status().isBlank() || query.status().equals(product.status()))
                .filter(product -> matchSalesBand(product, query.salesBand(), hotSalesThreshold, lowSalesThreshold, recent30DaySalesBySpu))
                .sorted(productComparator(query.sortBy(), query.sortOrder(), recent30DaySalesBySpu))
                .toList();

        PageResult<ProductSpu> pageResult = PageResult.of(filtered, query.page(), query.size());
        List<ProductSpu> pageProducts = pageResult.records();
        Map<Long, ProductSpu> productBySkuId = pageProducts.stream()
                .flatMap(product -> product.skus().stream().filter(sku -> sku.id() != null).map(sku -> Map.entry(sku.id(), product)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));
        Map<Long, Integer> stockTotalByProductId = stockFacade.stockListBySkuIds(productBySkuId.keySet().stream().toList()).stream()
                .collect(Collectors.groupingBy(stock -> productBySkuId.get(stock.skuId()).id(), LinkedHashMap::new, Collectors.summingInt(StockSnapshot::totalStock)));
        List<AdminProductPageRowView> rows = pageProducts.stream()
                .map(product -> {
                    int monthlySalesCount = monthlySales(product, recent30DaySalesBySpu);
                    String salesBand = resolveSalesBand(monthlySalesCount, hotSalesThreshold, lowSalesThreshold);
                    return productViewAssembler.toAdminProductPageRow(product, stockTotalByProductId.getOrDefault(product.id(), 0), monthlySalesCount, salesBand, salesBandLabel(salesBand));
                })
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

    private Comparator<ProductSpu> productComparator(String sortBy, String sortOrder, Map<Long, ProductSalesStatApplicationService.ProductSalesAggregate> recent30DaySalesBySpu) {
        Comparator<ProductSpu> comparator = switch (blank(sortBy) ? "id" : sortBy) {
            case "id" -> Comparator.comparing(ProductSpu::id, Comparator.nullsLast(Long::compareTo));
            case "name" -> Comparator.comparing(ProductSpu::name, Comparator.nullsLast(String::compareTo));
            case "categoryId" -> Comparator.comparing(ProductSpu::categoryId, Comparator.nullsLast(Long::compareTo));
            case "status" -> Comparator.comparing(ProductSpu::status, Comparator.nullsLast(String::compareTo));
            case "skuCount" -> Comparator.comparing(product -> product.skus() == null ? 0 : product.skus().size(), Comparator.nullsLast(Integer::compareTo));
            case "monthlySalesCount", "salesBand" -> Comparator.comparing(product -> monthlySales(product, recent30DaySalesBySpu), Comparator.nullsLast(Integer::compareTo));
            case "salesCount" -> Comparator.comparing(this::totalSales, Comparator.nullsLast(Integer::compareTo));
            default -> Comparator.comparing(ProductSpu::id, Comparator.nullsLast(Long::compareTo));
        };
        return "desc".equalsIgnoreCase(sortOrder) ? comparator.reversed() : comparator;
    }

    private boolean matchSalesBand(ProductSpu product, String salesBand, int hotSalesThreshold, int lowSalesThreshold, Map<Long, ProductSalesStatApplicationService.ProductSalesAggregate> recent30DaySalesBySpu) {
        if (blank(salesBand)) {
            return true;
        }
        return resolveSalesBand(monthlySales(product, recent30DaySalesBySpu), hotSalesThreshold, lowSalesThreshold).equalsIgnoreCase(salesBand);
    }

    private String resolveSalesBand(int monthlySalesCount, int hotSalesThreshold, int lowSalesThreshold) {
        if (monthlySalesCount >= hotSalesThreshold) {
            return "HOT";
        }
        if (monthlySalesCount <= lowSalesThreshold) {
            return "LOW";
        }
        return "NORMAL";
    }

    private String salesBandLabel(String salesBand) {
        return switch (salesBand) {
            case "HOT" -> "热销";
            case "LOW" -> "低销";
            default -> "常规";
        };
    }

    private int positiveOrDefault(Integer value, int defaultValue) {
        return value == null || value < 0 ? defaultValue : value;
    }

    private int monthlySales(ProductSpu product, Map<Long, ProductSalesStatApplicationService.ProductSalesAggregate> recent30DaySalesBySpu) {
        ProductSalesStatApplicationService.ProductSalesAggregate aggregate = recent30DaySalesBySpu.get(product.id());
        return aggregate == null ? 0 : aggregate.quantity();
    }

    private int totalSales(ProductSpu product) {
        return product.skus().stream()
                .map(sku -> sku.salesCount() == null ? 0 : sku.salesCount())
                .mapToInt(Integer::intValue)
                .sum();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record CacheEntry<T>(T value, Instant expiresAt) {
        static <T> CacheEntry<T> of(T value, Duration ttl) {
            return new CacheEntry<>(value, Instant.now().plus(ttl));
        }

        boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
