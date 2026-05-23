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

    public void refreshAdminSessionByAdminId(Long adminId, String nickname, String roleCode, List<String> permissions) {
        authSessionManager.refreshAdminSessionByAdminId(adminId, nickname, roleCode, permissions);
    }

    public AuthenticatedPrincipal currentPrincipal() {
        return authSessionManager.currentPrincipal();
    }

    public void disableUserSession(Long userId) {
        authSessionManager.disableUserSession(userId);
    }

    public void disableAdminSession(Long adminId) {
        authSessionManager.disableAdminSession(adminId);
    }

    public void enableUserSession(Long userId) {
        authSessionManager.enableUserSession(userId);
    }

    public boolean isUserDisabled(Long userId) {
        return authSessionManager.isUserDisabled(userId);
    }

    public void logout() {
        authSessionManager.logout();
    }
}
