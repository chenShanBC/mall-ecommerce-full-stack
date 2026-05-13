package com.mallfei.admin.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminAssignRoleRequest(
        @NotBlank(message = "角色编码不能为空") String roleCode,
        Boolean useDefaultPermissions
) {
}
