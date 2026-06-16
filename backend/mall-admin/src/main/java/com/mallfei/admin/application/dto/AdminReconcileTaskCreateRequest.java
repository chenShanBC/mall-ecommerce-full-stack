package com.mallfei.admin.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AdminReconcileTaskCreateRequest(
        @NotNull(message = "对账日期不能为空") LocalDate reconcileDate,
        @NotBlank(message = "渠道不能为空") String channel,
        String remark
) {
}
