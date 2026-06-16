package com.mallfei.stock.domain.service;

import com.mallfei.stock.domain.model.Stock;
import com.mallfei.stock.domain.model.StockConsistencyCheckResult;
import com.mallfei.stock.domain.model.StockConsistencySnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StockConsistencyDomainService {

    public StockConsistencyCheckResult check(Stock stock, long reservedQuantity, StockConsistencySnapshot redisSnapshot) {
        int expectedLocked = Math.toIntExact(Math.max(0, reservedQuantity));
        int expectedAvailable = Math.max(0, safe(stock.totalStock()) - expectedLocked);
        StockConsistencySnapshot stockSnapshot = StockConsistencySnapshot.of(stock.totalStock(), stock.lockedStock(), stock.availableStock(), "STOCK_TABLE");
        StockConsistencySnapshot expectedSnapshot = StockConsistencySnapshot.of(stock.totalStock(), expectedLocked, expectedAvailable, "DB_LOCK_RECORD_CALCULATED");
        List<String> differences = new ArrayList<>();
        compareStockSnapshot(stockSnapshot, expectedSnapshot, differences);
        compareRedisSnapshot(redisSnapshot, expectedSnapshot, differences);
        return StockConsistencyCheckResult.of(stock.skuId(), stockSnapshot, expectedSnapshot, redisSnapshot, differences);
    }

    private void compareStockSnapshot(StockConsistencySnapshot stockSnapshot,
                                      StockConsistencySnapshot expectedSnapshot,
                                      List<String> differences) {
        if (!equals(stockSnapshot.lockedStock(), expectedSnapshot.lockedStock())) {
            differences.add("库存表锁定库存与锁记录汇总不一致");
        }
        if (!equals(stockSnapshot.availableStock(), expectedSnapshot.availableStock())) {
            differences.add("库存表可用库存与理论可用库存不一致");
        }
        if (!equals(stockSnapshot.totalStock(), expectedSnapshot.totalStock())) {
            differences.add("库存表总库存与理论总库存不一致");
        }
    }

    private void compareRedisSnapshot(StockConsistencySnapshot redisSnapshot,
                                      StockConsistencySnapshot expectedSnapshot,
                                      List<String> differences) {
        if (redisSnapshot == null) {
            differences.add("Redis库存缓存不存在");
            return;
        }
        if (!equals(redisSnapshot.totalStock(), expectedSnapshot.totalStock())) {
            differences.add("Redis总库存与DB理论库存不一致");
        }
        if (!equals(redisSnapshot.lockedStock(), expectedSnapshot.lockedStock())) {
            differences.add("Redis锁定库存与DB理论库存不一致");
        }
        if (!equals(redisSnapshot.availableStock(), expectedSnapshot.availableStock())) {
            differences.add("Redis可用库存与DB理论库存不一致");
        }
    }

    private boolean equals(Integer left, Integer right) {
        return safe(left) == safe(right);
    }

    private int safe(Integer value) {
        return Math.max(0, value == null ? 0 : value);
    }
}
