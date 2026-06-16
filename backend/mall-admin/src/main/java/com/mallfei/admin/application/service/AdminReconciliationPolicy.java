package com.mallfei.admin.application.service;

import com.mallfei.admin.application.vo.AdminReconciliationActionView;
import com.mallfei.pay.domain.model.PayReconciliationRecord;

import java.util.ArrayList;
import java.util.List;

final class AdminReconciliationPolicy {

    static final String ACTION_RETRY_RECONCILE = "RETRY_RECONCILE";
    static final String ACTION_SYNC_PAY_STATUS = "SYNC_PAY_STATUS";
    static final String ACTION_REPAIR_ORDER_PAID = "REPAIR_ORDER_PAID";
    static final String ACTION_RESTORE_PENDING_PAYMENT = "RESTORE_PENDING_PAYMENT";
    static final String ACTION_CLOSE_AND_RELEASE_STOCK = "CLOSE_AND_RELEASE_STOCK";
    static final String ACTION_WAIT_CHANNEL_CONFIRM = "WAIT_CHANNEL_CONFIRM";
    static final String ACTION_WAIT_USER_REPAY = "WAIT_USER_REPAY";
    static final String ACTION_WAIT_REFUND = "WAIT_REFUND";
    static final String ACTION_MANUAL_REVIEW = "MANUAL_REVIEW";
    static final String ACTION_MARK_DONE = "MARK_DONE";
    static final String ACTION_IGNORE = "IGNORE";

    static final String STATUS_RECHECKING = "RECHECKING";
    static final String STATUS_WAIT_CHANNEL_CONFIRM = "WAIT_CHANNEL_CONFIRM";
    static final String STATUS_WAIT_USER_REPAY = "WAIT_USER_REPAY";
    static final String STATUS_WAIT_REFUND = "WAIT_REFUND";
    static final String STATUS_MANUAL_REVIEW = "MANUAL_REVIEW";

    private AdminReconciliationPolicy() {
    }

    static String category(PayReconciliationRecord record) {
        String diffType = value(record.diffType());
        if (PayReconciliationRecord.DIFF_NONE.equals(diffType)) {
            return "NORMAL";
        }
        if (PayReconciliationRecord.DIFF_LOCAL_PAYING_CHANNEL_SUCCESS.equals(diffType)
                || PayReconciliationRecord.DIFF_PAY_SUCCESS_ORDER_NOT_PAID.equals(diffType)
                || PayReconciliationRecord.DIFF_LONG_PLATFORM_SUCCESS_CHANNEL_MISSING.equals(diffType)) {
            return "LONG";
        }
        if (PayReconciliationRecord.DIFF_LOCAL_MISSING.equals(diffType)
                || PayReconciliationRecord.DIFF_CHANNEL_MISSING.equals(diffType)
                || PayReconciliationRecord.DIFF_UNPAID_ORDER_NEED_CLOSE_RELEASE.equals(diffType)
                || PayReconciliationRecord.DIFF_SHORT_CHANNEL_SUCCESS_PLATFORM_UNPAID.equals(diffType)
                || PayReconciliationRecord.DIFF_SHORT_PAID_ORDER_PAY_MISSING.equals(diffType)) {
            return "SHORT";
        }
        if (PayReconciliationRecord.DIFF_ORDER_STATUS_NOT_SYNCED.equals(diffType)) {
            return "STATUS";
        }
        if (diffType.contains("AMOUNT")) {
            return "AMOUNT";
        }
        if (diffType.contains("STATUS")) {
            return "STATUS";
        }
        return "MANUAL";
    }

    static String categoryLabel(String category) {
        return switch (value(category)) {
            case "NORMAL" -> "对平";
            case "LONG" -> "长款/长单";
            case "SHORT" -> "短款/短单";
            case "AMOUNT" -> "金额不一致";
            case "STATUS" -> "状态不一致";
            default -> "人工核查";
        };
    }

    static String suggestion(PayReconciliationRecord record) {
        String diffType = value(record.diffType());
        return switch (diffType) {
            case PayReconciliationRecord.DIFF_NONE -> "双方金额和状态一致，无需处置。";
            case PayReconciliationRecord.DIFF_LOCAL_PAYING_CHANNEL_SUCCESS, PayReconciliationRecord.DIFF_SHORT_CHANNEL_SUCCESS_PLATFORM_UNPAID -> "渠道已支付但本地支付单/订单未正确入账，建议同步支付状态并补偿订单，避免用户已付款却无法履约。";
            case PayReconciliationRecord.DIFF_PAY_SUCCESS_ORDER_NOT_PAID, PayReconciliationRecord.DIFF_ORDER_STATUS_NOT_SYNCED -> "支付单已成功但订单未支付，建议补偿订单为已支付，恢复后续履约。";
            case PayReconciliationRecord.DIFF_LONG_PLATFORM_SUCCESS_CHANNEL_MISSING -> "平台侧显示支付成功但渠道查询未确认收款，属于疑似长款/平台多记账风险；建议先重查渠道或调单，确认未收款时恢复待支付、关闭订单或联系用户补款。";
            case PayReconciliationRecord.DIFF_SHORT_PAID_ORDER_PAY_MISSING -> "订单已进入已支付/履约状态但缺少支付单，属于疑似短款/资损风险；建议冻结履约并人工核查，确认未收款时恢复待支付或关闭订单。";
            case PayReconciliationRecord.DIFF_UNPAID_ORDER_NEED_CLOSE_RELEASE -> "本地与渠道缺少有效收款证据，建议恢复待支付或关闭订单释放库存，避免继续履约造成资损。";
            case PayReconciliationRecord.DIFF_LOCAL_MISSING -> "本地缺少对应流水，请核查渠道/回调报文；确认用户已付款时补齐支付业务链路，无法归属时转人工或退款。";
            case PayReconciliationRecord.DIFF_CHANNEL_MISSING -> "渠道缺少对应成功流水，请重新查询渠道；确认未收款时冻结/关闭订单或联系用户补款。";
            case PayReconciliationRecord.DIFF_AMOUNT_MISMATCH, PayReconciliationRecord.DIFF_STATUS_AND_AMOUNT_MISMATCH -> "双方金额存在差异，禁止直接发货；请重新对账，必要时转待退款、待补款或人工审核。";
            case PayReconciliationRecord.DIFF_STATUS_MISMATCH -> "双方金额可核对但状态不一致，建议同步支付状态或补偿订单状态。";
            default -> "请结合订单、支付单、渠道报文和回调记录人工核查，并选择对应处置动作。";
        };
    }

    static List<AdminReconciliationActionView> actions(PayReconciliationRecord record) {
        if (record.consistent() != null && record.consistent()) {
            return List.of();
        }
        if (!PayReconciliationRecord.REPAIR_PENDING.equals(record.repairStatus())) {
            return List.of();
        }
        String diffType = value(record.diffType());
        List<AdminReconciliationActionView> actions = new ArrayList<>();
        add(actions, ACTION_RETRY_RECONCILE, "重新对账", "重新查询渠道并生成最新对账结果", "primary", false);
        switch (diffType) {
            case PayReconciliationRecord.DIFF_LOCAL_PAYING_CHANNEL_SUCCESS, PayReconciliationRecord.DIFF_SHORT_CHANNEL_SUCCESS_PLATFORM_UNPAID -> {
                add(actions, ACTION_SYNC_PAY_STATUS, "同步支付状态", "渠道已支付时同步本地支付单并补偿订单", "success", false);
                add(actions, ACTION_REPAIR_ORDER_PAID, "补偿订单已支付", "支付单成功但订单未支付时补偿订单状态", "success", false);
                add(actions, ACTION_WAIT_CHANNEL_CONFIRM, "待渠道调单", "渠道结果仍不确定，进入人工调单状态", "warning", false);
            }
            case PayReconciliationRecord.DIFF_PAY_SUCCESS_ORDER_NOT_PAID, PayReconciliationRecord.DIFF_ORDER_STATUS_NOT_SYNCED -> add(actions, ACTION_REPAIR_ORDER_PAID, "补偿订单已支付", "支付单成功但订单未支付时补偿订单状态", "success", false);
            case PayReconciliationRecord.DIFF_LONG_PLATFORM_SUCCESS_CHANNEL_MISSING -> {
                add(actions, ACTION_WAIT_CHANNEL_CONFIRM, "待渠道调单", "先向渠道核查原始报文，避免把查询失败误判为未收款", "warning", false);
                add(actions, ACTION_RESTORE_PENDING_PAYMENT, "恢复待支付", "确认渠道未收款但允许用户重新支付", "warning", false);
                add(actions, ACTION_CLOSE_AND_RELEASE_STOCK, "关闭并释放库存", "确认未收款且不再追缴时关闭订单止损", "danger", true);
                add(actions, ACTION_WAIT_USER_REPAY, "待用户补款", "订单已履约或需追回少收款时进入补款/追缴", "warning", false);
            }
            case PayReconciliationRecord.DIFF_SHORT_PAID_ORDER_PAY_MISSING -> {
                add(actions, ACTION_MANUAL_REVIEW, "人工审核", "缺少支付单但订单已支付，需核查是否误操作或历史脏数据", "warning", false);
                add(actions, ACTION_RESTORE_PENDING_PAYMENT, "恢复待支付", "确认未收款时恢复待支付", "warning", false);
                add(actions, ACTION_CLOSE_AND_RELEASE_STOCK, "关闭并释放库存", "确认无收款证据时关闭订单止损", "danger", true);
            }
            case PayReconciliationRecord.DIFF_UNPAID_ORDER_NEED_CLOSE_RELEASE -> {
                add(actions, ACTION_RESTORE_PENDING_PAYMENT, "恢复待支付", "确认未收款但仍允许用户继续支付", "warning", false);
                add(actions, ACTION_CLOSE_AND_RELEASE_STOCK, "关闭并释放库存", "确认无收款证据时关闭订单止损", "danger", true);
            }
            case PayReconciliationRecord.DIFF_LOCAL_MISSING -> {
                add(actions, ACTION_SYNC_PAY_STATUS, "补偿支付链路", "尝试根据渠道结果同步本地支付状态", "success", false);
                add(actions, ACTION_WAIT_REFUND, "待退款", "无法归属或需退还用户时进入待退款", "warning", false);
                add(actions, ACTION_WAIT_CHANNEL_CONFIRM, "待渠道调单", "保留渠道流水并进入人工调单", "warning", false);
            }
            case PayReconciliationRecord.DIFF_CHANNEL_MISSING -> {
                add(actions, ACTION_WAIT_USER_REPAY, "待用户补款", "确认少收款时联系用户重新支付", "warning", false);
                add(actions, ACTION_CLOSE_AND_RELEASE_STOCK, "关闭并释放库存", "确认未收款且不再追缴时关闭订单", "danger", true);
                add(actions, ACTION_WAIT_CHANNEL_CONFIRM, "待渠道调单", "先向渠道核查原始报文", "warning", false);
            }
            case PayReconciliationRecord.DIFF_AMOUNT_MISMATCH, PayReconciliationRecord.DIFF_STATUS_AND_AMOUNT_MISMATCH -> {
                add(actions, ACTION_WAIT_REFUND, "待退款", "平台多收或找零未退时进入待退款", "warning", false);
                add(actions, ACTION_WAIT_USER_REPAY, "待补款", "平台少收时进入用户补款/追缴", "warning", false);
                add(actions, ACTION_MANUAL_REVIEW, "人工审核", "金额敏感差异转财务人工审核", "warning", false);
            }
            case PayReconciliationRecord.DIFF_STATUS_MISMATCH -> {
                add(actions, ACTION_SYNC_PAY_STATUS, "同步支付状态", "重新同步支付单和订单状态", "success", false);
                add(actions, ACTION_REPAIR_ORDER_PAID, "补偿订单状态", "确认已收款后补偿订单为已支付", "success", false);
            }
            default -> add(actions, ACTION_MANUAL_REVIEW, "人工审核", "暂不自动处置的差异转人工审核", "warning", false);
        }
        add(actions, ACTION_MARK_DONE, "确认已解决", "业务已在线上或线下处理完成后归档", "info", false);
        add(actions, ACTION_IGNORE, "忽略归档", "确认无需处理或测试数据时忽略", "info", true);
        return actions;
    }

    private static void add(List<AdminReconciliationActionView> actions, String action, String label, String description, String type, boolean danger) {
        actions.add(new AdminReconciliationActionView(action, label, description, type, danger));
    }

    private static String value(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
