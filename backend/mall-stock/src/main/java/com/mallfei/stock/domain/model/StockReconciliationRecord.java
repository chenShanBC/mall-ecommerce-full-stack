package com.mallfei.stock.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record StockReconciliationRecord(
        Long id,
        Long skuId,
        String status,
        String severity,
        StockConsistencySnapshot stockSnapshot,
        StockConsistencySnapshot expectedSnapshot,
        StockConsistencySnapshot redisSnapshot,
        List<String> differences,
        String repairStatus,
        String repairRemark,
        LocalDateTime checkedAt,
        LocalDateTime repairedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static final String STATUS_CONSISTENT = "CONSISTENT";
    public static final String STATUS_INCONSISTENT = "INCONSISTENT";
    public static final String STATUS_REPAIRED = "REPAIRED";
    public static final String STATUS_IGNORED = "IGNORED";
    public static final String REPAIR_NONE = "NONE";
    public static final String REPAIR_PENDING = "PENDING";
    public static final String REPAIR_DONE = "DONE";
    public static final String REPAIR_IGNORED = "IGNORED";

    public static StockReconciliationRecord fromCheck(StockConsistencyCheckResult result) {
        LocalDateTime now = LocalDateTime.now();
        return new StockReconciliationRecord(
                null,
                result.skuId(),
                result.status(),
                result.severity(),
                result.stockSnapshot(),
                result.expectedSnapshot(),
                result.redisSnapshot(),
                result.differences(),
                result.consistent() ? REPAIR_NONE : REPAIR_PENDING,
                null,
                result.checkedAt(),
                null,
                now,
                now
        );
    }

    public StockReconciliationRecord repaired(String remark) {
        LocalDateTime now = LocalDateTime.now();
        return new StockReconciliationRecord(id, skuId, STATUS_REPAIRED, severity, stockSnapshot, expectedSnapshot, redisSnapshot, differences, REPAIR_DONE, remark, checkedAt, now, createdAt, now);
    }

    public StockReconciliationRecord ignored(String remark) {
        LocalDateTime now = LocalDateTime.now();
        return new StockReconciliationRecord(id, skuId, STATUS_IGNORED, severity, stockSnapshot, expectedSnapshot, redisSnapshot, differences, REPAIR_IGNORED, remark, checkedAt, now, createdAt, now);
    }
}
