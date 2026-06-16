package com.mallfei.admin.application.vo;

import java.time.LocalDateTime;
import java.util.List;

public record AdminRefundView(
        Long id,
        String refundNo,
        String orderNo,
        Long userId,
        Long refundAmountCent,
        String channelRefundNo,
        String refundStatus,
        String refundReason,
        String failReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AdminRefundItemView> items
) {
}
