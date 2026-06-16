package com.mallfei.stock.application.vo;

import java.time.LocalDateTime;
import java.util.List;

public record StockReconciliationRecordView(
        Long id,
        Long skuId,
        String status,
        String severity,
        StockConsistencySnapshotView stockSnapshot,
        StockConsistencySnapshotView expectedSnapshot,
        StockConsistencySnapshotView redisSnapshot,
        List<String> differences,
        String repairStatus,
        String repairRemark,
        LocalDateTime checkedAt,
        LocalDateTime repairedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
