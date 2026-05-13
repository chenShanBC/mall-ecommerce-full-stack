package com.mallfei.admin.application.vo;

import java.util.List;

public record AdminRoleView(
        String code,
        String name,
        List<String> defaultPermissions,
        boolean builtIn
) {
}
