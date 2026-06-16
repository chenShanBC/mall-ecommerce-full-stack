package com.mallfei.product.application.service;

import com.mallfei.product.infrastructure.persistence.mapper.ProductSalesDailyStatMapper;
import com.mallfei.product.infrastructure.persistence.mapper.ProductSalesStatEventMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductSalesStatApplicationService {
    private static final String EVENT_TYPE_ORDER_COMPLETED = "ORDER_COMPLETED";
    private static final String DEFAULT_SALE_CHANNEL = "NORMAL";

    private final ProductSalesDailyStatMapper productSalesDailyStatMapper;
    private final ProductSalesStatEventMapper productSalesStatEventMapper;

    public ProductSalesStatApplicationService(ProductSalesDailyStatMapper productSalesDailyStatMapper,
                                              ProductSalesStatEventMapper productSalesStatEventMapper) {
        this.productSalesDailyStatMapper = productSalesDailyStatMapper;
        this.productSalesStatEventMapper = productSalesStatEventMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean recordOrderCompleted(String orderNo, LocalDateTime completedAt, List<OrderCompletedSalesItem> items) {
        if (orderNo == null || orderNo.isBlank() || items == null || items.isEmpty()) {
            return false;
        }
        String eventKey = EVENT_TYPE_ORDER_COMPLETED + ":" + orderNo;
        if (productSalesStatEventMapper.insertIgnore(eventKey, EVENT_TYPE_ORDER_COMPLETED, orderNo) <= 0) {
            return false;
        }
        LocalDate statDate = (completedAt == null ? LocalDateTime.now() : completedAt).toLocalDate();
        for (OrderCompletedSalesItem item : mergeItems(items)) {
            if (item.spuId() == null || item.skuId() == null || item.quantity() == null || item.quantity() <= 0) {
                continue;
            }
            long amountCent = item.amountCent() == null ? 0L : Math.max(item.amountCent(), 0L);
            productSalesDailyStatMapper.incrementCompleted(statDate, item.spuId(), item.skuId(), DEFAULT_SALE_CHANNEL, item.quantity(), amountCent);
        }
        return true;
    }

    public Map<Long, ProductSalesAggregate> recent30DaySalesBySpuIds(List<Long> spuIds) {
        if (spuIds == null || spuIds.isEmpty()) {
            return Map.of();
        }
        String spuIdsSql = spuIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        if (spuIdsSql.isBlank()) {
            return Map.of();
        }
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);
        return toAggregateMap(productSalesDailyStatMapper.aggregateBySpuIds(startDate, endDate, spuIdsSql));
    }

    public Map<Long, ProductSalesAggregate> recent30DaySalesBySpu() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);
        return toAggregateMap(productSalesDailyStatMapper.aggregateBySpu(startDate, endDate));
    }

    public long recent7DaySalesCount() {
        LocalDate endDate = LocalDate.now();
        return productSalesDailyStatMapper.sumQuantity(endDate.minusDays(6), endDate);
    }

    public long recent30DaySalesCount() {
        LocalDate endDate = LocalDate.now();
        return productSalesDailyStatMapper.sumQuantity(endDate.minusDays(29), endDate);
    }

    public long currentMonthSalesCount() {
        LocalDate endDate = LocalDate.now();
        return productSalesDailyStatMapper.sumQuantity(endDate.withDayOfMonth(1), endDate);
    }

    public long recent30DaySalesAmountCent() {
        LocalDate endDate = LocalDate.now();
        return productSalesDailyStatMapper.sumAmountCent(endDate.minusDays(29), endDate);
    }

    public List<ProductSalesMonthlyAggregate> recentMonthlySalesTrend(int monthCount) {
        int safeMonthCount = Math.min(Math.max(monthCount, 1), 12);
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.withDayOfMonth(1).minusMonths(safeMonthCount - 1L);
        Map<String, ProductSalesMonthlyAggregate> aggregateByMonth = new LinkedHashMap<>();
        for (ProductSalesDailyStatMapper.ProductSalesMonthlyAggregateRow row : productSalesDailyStatMapper.aggregateMonthly(startDate, today)) {
            if (row.getMonth() == null || row.getMonth().isBlank()) {
                continue;
            }
            aggregateByMonth.put(row.getMonth(), new ProductSalesMonthlyAggregate(row.getMonth(), row.getQuantity() == null ? 0L : row.getQuantity(), row.getAmountCent() == null ? 0L : row.getAmountCent()));
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        List<ProductSalesMonthlyAggregate> result = new java.util.ArrayList<>();
        for (int index = safeMonthCount - 1; index >= 0; index--) {
            String month = today.minusMonths(index).format(formatter);
            result.add(aggregateByMonth.getOrDefault(month, new ProductSalesMonthlyAggregate(month, 0L, 0L)));
        }
        return result;
    }

    private Map<Long, ProductSalesAggregate> toAggregateMap(List<ProductSalesDailyStatMapper.ProductSalesAggregateRow> rows) {
        Map<Long, ProductSalesAggregate> result = new LinkedHashMap<>();
        for (ProductSalesDailyStatMapper.ProductSalesAggregateRow row : rows) {
            if (row.getSpuId() == null) {
                continue;
            }
            result.put(row.getSpuId(), new ProductSalesAggregate(row.getSpuId(), row.getQuantity() == null ? 0 : row.getQuantity(), row.getAmountCent() == null ? 0L : row.getAmountCent()));
        }
        return result;
    }

    private List<OrderCompletedSalesItem> mergeItems(List<OrderCompletedSalesItem> items) {
        Map<String, OrderCompletedSalesItem> merged = new LinkedHashMap<>();
        for (OrderCompletedSalesItem item : items) {
            if (item == null || item.spuId() == null || item.skuId() == null) {
                continue;
            }
            String key = item.spuId() + ":" + item.skuId();
            OrderCompletedSalesItem existing = merged.get(key);
            if (existing == null) {
                merged.put(key, item);
                continue;
            }
            merged.put(key, new OrderCompletedSalesItem(
                    item.spuId(),
                    item.skuId(),
                    safeQuantity(existing.quantity()) + safeQuantity(item.quantity()),
                    safeAmount(existing.amountCent()) + safeAmount(item.amountCent())
            ));
        }
        return merged.values().stream().toList();
    }

    private int safeQuantity(Integer quantity) {
        return quantity == null ? 0 : Math.max(quantity, 0);
    }

    private long safeAmount(Long amountCent) {
        return amountCent == null ? 0L : Math.max(amountCent, 0L);
    }

    public record OrderCompletedSalesItem(Long spuId, Long skuId, Integer quantity, Long amountCent) {}
    public record ProductSalesAggregate(Long spuId, int quantity, long amountCent) {}
    public record ProductSalesMonthlyAggregate(String month, long quantity, long amountCent) {}
}
