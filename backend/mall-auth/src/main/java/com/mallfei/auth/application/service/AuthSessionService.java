package com.mallfei.auth.application.service;

import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.enums.IdentityType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthSessionService {

    private final com.mallfei.auth.infrastructure.auth.AuthSessionManager authSessionManager;

    public AuthSessionService(com.mallfei.auth.infrastructure.auth.AuthSessionManager authSessionManager) {
        this.authSessionManager = authSessionManager;
    }

    public String createSession(Long principalId, String account, IdentityType identityType, String nickname, String avatar, String roleCode, List<String> permissions) {
        return authSessionManager.createSession(principalId, account, identityType, nickname, avatar, roleCode, permissions).token();
    }

    public void refreshAdminSession(String nickname, String roleCode, List<String> permissions) {
        authSessionManager.refreshAdminSession(nickname, roleCode, permissions);
    }

    public AuthenticatedPrincipal currentPrincipal() {
        return authSessionManager.currentPrincipal();
    }

    public void logout() {
        authSessionManager.logout();
    }
}
