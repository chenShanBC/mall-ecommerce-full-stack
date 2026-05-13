package com.mallfei.auth.application.service;

import com.mallfei.auth.domain.service.AuthDomainService;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.enums.IdentityType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthApplicationService {

    private final AuthSessionService authSessionService;
    private final AuthDomainService authDomainService;

    public AuthApplicationService(AuthSessionService authSessionService,
                                  AuthDomainService authDomainService) {
        this.authSessionService = authSessionService;
        this.authDomainService = authDomainService;
    }

    public String createLoginSession(Long principalId, String account, IdentityType identityType, String nickname, String avatar, String roleCode, List<String> permissions) {
        return authSessionService.createSession(principalId, account, identityType, nickname, avatar, roleCode, permissions);
    }

    public void refreshAdminSession(String nickname, String roleCode, List<String> permissions) {
        authSessionService.refreshAdminSession(nickname, roleCode, permissions);
    }

    public AuthenticatedPrincipal currentPrincipal() {
        return authSessionService.currentPrincipal();
    }

    public AuthenticatedPrincipal currentRequiredPrincipal() {
        AuthenticatedPrincipal principal = currentPrincipal();
        authDomainService.ensureLoggedIn(principal);
        return principal;
    }

    public void logout() {
        authSessionService.logout();
    }
}
