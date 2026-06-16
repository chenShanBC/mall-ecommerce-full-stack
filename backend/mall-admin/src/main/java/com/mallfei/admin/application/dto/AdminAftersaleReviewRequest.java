package com.mallfei.admin.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "AdminAftersaleReviewRequest", description = "后台售后审核请求")
public record AdminAftersaleReviewRequest(
        @Schema(description = "审核动作", example = "APPROVE")
        @NotBlank(message = "审核动作不能为空")
        String action,
        @Schema(description = "审核说明/驳回原因", example = "退款原因不符合售后规则")
        String reason
) {
}
