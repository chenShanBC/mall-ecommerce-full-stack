package com.mallfei.auth.facade;

import com.mallfei.auth.application.service.AuthApplicationService;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.enums.IdentityType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuthFacade {

    private final AuthApplicationService authApplicationService;

    public AuthFacade(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    public String createLoginSession(Long principalId, String account, IdentityType identityType, String nickname, String avatar, String roleCode, List<String> permissions) {
        return authApplicationService.createLoginSession(principalId, account, identityType, nickname, avatar, roleCode, permissions);
    }

    public void refreshAdminSession(String nickname, String roleCode, List<String> permissions) {
        authApplicationService.refreshAdminSession(nickname, roleCode, permissions);
    }

    public AuthenticatedPrincipal currentPrincipal() {
        return authApplicationService.currentPrincipal();
    }

    public AuthenticatedPrincipal currentRequiredPrincipal() {
        return authApplicationService.currentRequiredPrincipal();
    }

    public void logout() {
        authApplicationService.logout();
    }
}
