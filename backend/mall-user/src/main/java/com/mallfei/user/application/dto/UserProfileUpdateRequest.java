package com.mallfei.user.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "UserProfileUpdateRequest", description = "用户资料更新请求")
public record UserProfileUpdateRequest(
        @Schema(description = "用户昵称", example = "陈子涵")
        @NotBlank(message = "昵称不能为空")
        @Size(max = 20, message = "昵称长度不能超过20个字符") String nickname,
        @Schema(description = "头像地址", example = "https://example.com/avatar.png")
        @Size(max = 255, message = "头像地址长度不能超过255个字符") String avatarUrl
) {
}
