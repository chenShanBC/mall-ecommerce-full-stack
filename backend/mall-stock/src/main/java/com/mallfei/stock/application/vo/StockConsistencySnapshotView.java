package com.mallfei.stock.application.vo;

public record StockConsistencySnapshotView(
        Integer totalStock,
        Integer lockedStock,
        Integer availableStock,
        String source
) {
}
