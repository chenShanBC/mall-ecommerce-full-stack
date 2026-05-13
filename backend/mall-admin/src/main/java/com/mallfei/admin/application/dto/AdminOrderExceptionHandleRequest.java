package com.mallfei.admin.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminOrderExceptionHandleRequest(
        String receiverName,
        String receiverPhone,
        String receiverDetailAddress,
        @NotBlank(message = "异常类型不能为空") String exceptionType,
        @NotBlank(message = "处理备注不能为空") String note
) {
}
