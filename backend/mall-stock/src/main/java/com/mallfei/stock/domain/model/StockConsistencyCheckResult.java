package com.mallfei.stock.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record StockConsistencyCheckResult(
        Long skuId,
        boolean consistent,
        String status,
        String severity,
        StockConsistencySnapshot stockSnapshot,
        StockConsistencySnapshot expectedSnapshot,
        StockConsistencySnapshot redisSnapshot,
        List<String> differences,
        LocalDateTime checkedAt
) {

    public static StockConsistencyCheckResult of(Long skuId,
                                                 StockConsistencySnapshot stockSnapshot,
                                                 StockConsistencySnapshot expectedSnapshot,
                                                 StockConsistencySnapshot redisSnapshot,
                                                 List<String> differences) {
        boolean consistent = differences == null || differences.isEmpty();
        return new StockConsistencyCheckResult(
                skuId,
                consistent,
                consistent ? "CONSISTENT" : "INCONSISTENT",
                consistent ? "NONE" : "WARNING",
                stockSnapshot,
                expectedSnapshot,
                redisSnapshot,
                differences == null ? List.of() : List.copyOf(differences),
                LocalDateTime.now()
        );
    }
}
