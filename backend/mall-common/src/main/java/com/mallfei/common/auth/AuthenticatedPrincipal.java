package com.mallfei.common.auth;

import com.mallfei.common.enums.IdentityType;

import java.util.List;

public record AuthenticatedPrincipal(
        String loginId,
        Long principalId,
        String account,
        IdentityType identityType,
        String nickname,
        String avatar,
        String token,
        String roleCode,
        List<String> permissions
) {

    public boolean isAdmin() {
        return identityType == IdentityType.ADMIN;
    }

    public boolean isUser() {
        return identityType == IdentityType.USER;
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}
