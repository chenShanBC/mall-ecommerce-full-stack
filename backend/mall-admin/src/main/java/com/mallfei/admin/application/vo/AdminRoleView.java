package com.mallfei.admin.application.vo;

import java.util.List;

public record AdminRoleView(
        String code,
        String name,
        List<String> defaultPermissions,
        List<String> permissionScope,
        boolean builtIn
) {
}
