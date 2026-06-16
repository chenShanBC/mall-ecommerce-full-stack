package com.mallfei.stock.domain.model;

import com.mallfei.common.exception.BusinessException;

import java.util.Arrays;

public enum StockAdjustmentType {
    REPLENISH("补货入库", "增加总库存与可用库存，锁定库存不变"),
    INVENTORY_GAIN("盘盈", "增加总库存与可用库存，锁定库存不变"),
    INVENTORY_LOSS("盘亏", "减少总库存与可用库存，锁定库存不变"),
    MANUAL_UNLOCK("手动解锁", "减少锁定库存并增加可用库存，总库存不变"),
    FORCE_DEDUCT("强制扣减", "异常订单处理，优先扣减可用库存，不足时扣减锁定库存"),
    OTHER("其他", "兼容历史人工调整，需保证库存守恒");

    private final String label;
    private final String description;

    StockAdjustmentType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public static StockAdjustmentType from(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> BusinessException.badRequest("不支持的库存调整类型: " + value));
    }
}
