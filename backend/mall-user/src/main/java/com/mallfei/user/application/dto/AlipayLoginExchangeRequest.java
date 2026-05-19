package com.mallfei.user.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AlipayLoginExchangeRequest(
        @NotBlank(message = "loginTicket不能为空")
        String loginTicket
) {
}
