package com.mallfei.admin.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminOrderExceptionHandleRequest(
        String receiverName,
        String receiverPhone,
        String receiverProvinceName,
        String receiverCityName,
        String receiverDistrictName,
        String receiverDetailAddress,
        @NotBlank(message = "异常类型不能为空") String exceptionType,
        String negotiationAction,
        Long orderItemId,
        Long targetSkuId,
        String priceDifferenceHandleType,
        String note
) {
}
