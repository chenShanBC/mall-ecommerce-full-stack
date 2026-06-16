package com.mallfei.stock.domain.model;

import com.mallfei.common.exception.BusinessException;

public record StockAdjustmentCommand(
        StockAdjustmentType type,
        Integer changeQuantity,
        Integer targetTotalStock,
        Integer targetAvailableStock,
        Integer targetLockedStock,
        String reason,
        String remark
) {
    public StockAdjustmentCommand {
        type = type == null ? StockAdjustmentType.OTHER : type;
        reason = reason == null ? "" : reason.trim();
        remark = remark == null ? "" : remark.trim();
        validateReason(type, reason);
    }

    public static StockAdjustmentCommand business(StockAdjustmentType type, Integer changeQuantity, String reason, String remark) {
        if (changeQuantity == null || changeQuantity <= 0) {
            throw BusinessException.badRequest("调整数量必须大于 0");
        }
        return new StockAdjustmentCommand(type, changeQuantity, null, null, null, reason, remark);
    }

    public static StockAdjustmentCommand direct(Integer totalStock, Integer availableStock, Integer lockedStock, String reason) {
        return new StockAdjustmentCommand(StockAdjustmentType.OTHER, null, totalStock, availableStock, lockedStock, reason, null);
    }

    public String operationLabel() {
        return type.label();
    }

    private static void validateReason(StockAdjustmentType type, String reason) {
        if (reason.isBlank()) {
            return;
        }
        boolean allowed = switch (type) {
            case REPLENISH -> reason.equals("采购入库") || reason.equals("退货入库") || reason.equals("供应商赠品入库") || reason.equals("其他");
            case INVENTORY_GAIN -> reason.equals("盘点盘盈") || reason.equals("数据校正") || reason.equals("其他");
            case INVENTORY_LOSS -> reason.equals("盘点盘亏") || reason.equals("商品破损报损") || reason.equals("商品过期报损") || reason.equals("丢失报损") || reason.equals("其他");
            case MANUAL_UNLOCK -> reason.equals("订单取消释放") || reason.equals("支付超时释放") || reason.equals("锁定异常释放") || reason.equals("其他");
            case FORCE_DEDUCT -> reason.equals("订单差异处理") || reason.equals("库存差异修正") || reason.equals("线下销售补录") || reason.equals("其他");
            case OTHER -> reason.equals("数据校正") || reason.equals("历史数据迁移") || reason.equals("其他");
        };
        if (!allowed) {
            throw BusinessException.badRequest("调整原因与调整类型不匹配");
        }
    }

    public String auditRemark() {
        StringBuilder builder = new StringBuilder(type.label());
        if (!reason.isBlank()) {
            builder.append(" - ").append(reason);
        }
        if (!remark.isBlank()) {
            builder.append("：").append(remark);
        }
        return builder.toString();
    }
}
