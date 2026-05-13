package com.mallfei.admin.application.vo;

public record AdminOrderOperationResultView(
        String orderNo,
        String operationType,
        String result,
        String message
) {
}
