package com.mallfei.admin.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUpdateProfileRequest(
        @NotBlank(message = "昵称不能为空") String nickname
) {
}
