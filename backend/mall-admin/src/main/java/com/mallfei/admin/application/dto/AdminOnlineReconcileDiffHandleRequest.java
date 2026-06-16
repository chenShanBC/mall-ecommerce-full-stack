package com.mallfei.admin.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminOnlineReconcileDiffHandleRequest(
        @NotBlank(message = "处理动作不能为空") String action,
        String remark
) {
}
