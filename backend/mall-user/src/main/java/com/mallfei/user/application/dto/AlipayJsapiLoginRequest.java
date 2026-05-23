package com.mallfei.user.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AlipayJsapiLoginRequest(
        @NotBlank(message = "authCode不能为空")
        String authCode
) {
}
