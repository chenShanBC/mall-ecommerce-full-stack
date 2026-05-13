package com.mallfei.admin.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminStockWarningHandleRequest(
        @NotBlank(message = "处理动作不能为空") String action,
        @NotBlank(message = "处理说明不能为空") String note
) {
}
