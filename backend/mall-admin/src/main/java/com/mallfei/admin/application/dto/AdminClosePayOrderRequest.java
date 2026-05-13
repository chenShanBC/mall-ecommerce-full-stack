package com.mallfei.admin.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminClosePayOrderRequest(
        @NotBlank(message = "关闭原因不能为空") String reason
) {
}
