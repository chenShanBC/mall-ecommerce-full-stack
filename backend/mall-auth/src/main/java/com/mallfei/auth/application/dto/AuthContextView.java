package com.mallfei.auth.application.dto;

import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.enums.IdentityType;

import java.util.List;

public record AuthContextView(
        String loginId,
        Long principalId,
        String account,
        IdentityType identityType,
        boolean user,
        boolean admin,
        String roleCode,
        List<String> permissions
) {

    public static AuthContextView from(AuthenticatedPrincipal principal) {
        return new AuthContextView(
                principal.loginId(),
                principal.principalId(),
                principal.account(),
                principal.identityType(),
                principal.isUser(),
                principal.isAdmin(),
                principal.roleCode(),
                principal.permissions()
        );
    }
}
