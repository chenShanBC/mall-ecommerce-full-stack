package com.mallfei.stock.domain.model;

public record StockConsistencySnapshot(
        Integer totalStock,
        Integer lockedStock,
        Integer availableStock,
        String source
) {

    public static StockConsistencySnapshot of(Integer totalStock, Integer lockedStock, Integer availableStock, String source) {
        return new StockConsistencySnapshot(safe(totalStock), safe(lockedStock), safe(availableStock), source);
    }

    private static int safe(Integer value) {
        return Math.max(0, value == null ? 0 : value);
    }
}
