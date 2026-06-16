package com.mallfei.stock.application.vo;

import java.time.LocalDateTime;
import java.util.List;

public record StockConsistencyCheckView(
        Long skuId,
        boolean consistent,
        String status,
        String severity,
        StockConsistencySnapshotView stockSnapshot,
        StockConsistencySnapshotView expectedSnapshot,
        StockConsistencySnapshotView redisSnapshot,
        List<String> differences,
        LocalDateTime checkedAt
) {
}
