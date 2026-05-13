package com.mallfei.admin.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminProductViolationHandleRequest(
        @NotBlank(message = "处理动作不能为空") String action,
        @NotBlank(message = "处理原因不能为空") String reason
) {
}
