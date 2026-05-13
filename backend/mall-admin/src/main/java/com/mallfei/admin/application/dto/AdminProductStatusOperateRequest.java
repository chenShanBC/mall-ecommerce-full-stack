package com.mallfei.admin.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminProductStatusOperateRequest(
        @Pattern(regexp = "ONLINE|OFFLINE", message = "商品状态仅支持 ONLINE 或 OFFLINE")
        @NotBlank(message = "商品状态不能为空") String status,
        @NotBlank(message = "操作原因不能为空") String reason
) {
}
