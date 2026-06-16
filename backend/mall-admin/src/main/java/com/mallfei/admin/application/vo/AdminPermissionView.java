package com.mallfei.admin.application.vo;

public record AdminPermissionView(
        String code,
        String name,
        String groupCode,
        String groupName,
        boolean sensitive
) {
}
