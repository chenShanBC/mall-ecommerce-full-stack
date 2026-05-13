package com.mallfei.admin.domain.model;

import java.util.List;

public record AdminRole(
        String code,
        String name,
        List<String> defaultPermissions,
        boolean builtIn
) {
}
