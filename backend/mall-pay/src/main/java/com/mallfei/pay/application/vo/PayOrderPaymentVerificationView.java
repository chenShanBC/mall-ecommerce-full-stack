package com.mallfei.pay.application.vo;

import java.time.LocalDateTime;
import java.util.List;

public record PayOrderPaymentVerificationView(
        String orderNo,
        String orderStatus,
        Long orderPayAmountCent,
        String payOrderNo,
        String localPayStatus,
        String payChannel,
        String transactionNo,
        Long payAmountCent,
        String channelPayStatus,
        boolean channelKnown,
        Boolean channelPaid,
        String callbackProcessStatus,
        String callbackTradeStatus,
        boolean callbackVerified,
        boolean payOrderExists,
        boolean amountConsistent,
        String conclusion,
        String riskLevel,
        String suggestedAction,
        List<String> allowedActions,
        String message,
        LocalDateTime verifiedAt
) {
    public static final String ACTION_CONFIRM_PAID = "CONFIRM_PAID";
    public static final String ACTION_TRANSFER_ORDER_REPAIR = "TRANSFER_ORDER_REPAIR";
    public static final String ACTION_RESTORE_PENDING_PAYMENT = "RESTORE_PENDING_PAYMENT";
    public static final String ACTION_TRANSFER_CLOSE_RELEASE = "TRANSFER_CLOSE_RELEASE";
    public static final String ACTION_CLOSE_AND_RELEASE_STOCK = "CLOSE_AND_RELEASE_STOCK";
    public static final String ACTION_TRANSFER_PAY_SYNC = "TRANSFER_PAY_SYNC";
    public static final String ACTION_TRANSFER_AMOUNT_RECONCILE = "TRANSFER_AMOUNT_RECONCILE";
}
