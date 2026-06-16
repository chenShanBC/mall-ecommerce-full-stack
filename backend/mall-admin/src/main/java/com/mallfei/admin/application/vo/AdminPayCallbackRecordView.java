package com.mallfei.admin.application.vo;

import java.time.LocalDateTime;

public record AdminPayCallbackRecordView(
        Long id,
        String channel,
        String callbackType,
        String payOrderNo,
        String refundNo,
        String orderNo,
        String outTradeNo,
        String transactionNo,
        Long amountCent,
        String tradeStatus,
        boolean verified,
        String processStatus,
        String failReason,
        LocalDateTime callbackTime,
        LocalDateTime processedAt,
        LocalDateTime createdAt
) {
}
