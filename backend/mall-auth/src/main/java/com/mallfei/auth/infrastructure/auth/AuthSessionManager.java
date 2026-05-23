package com.mallfei.auth.infrastructure.auth;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallfei.auth.infrastructure.websocket.ForceLogoutWebSocketHandler;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.enums.IdentityType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class AuthSessionManager {

    private static final String SESSION_KEY_ACCOUNT = "account";
    private static final String SESSION_KEY_PRINCIPAL_ID = "principalId";
    private static final String SESSION_KEY_IDENTITY_TYPE = "identityType";
    private static final String SESSION_KEY_NICKNAME = "nickname";
    private static final String SESSION_KEY_AVATAR = "avatar";
    private static final String SESSION_KEY_ROLE_CODE = "roleCode";
    private static final String SESSION_KEY_PERMISSIONS = "permissions";
    private static final String DISABLED_USER_BLACKLIST_KEY_PREFIX = "auth:user:disabled:";
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ForceLogoutWebSocketHandler forceLogoutWebSocketHandler;

    public AuthSessionManager(ObjectMapper objectMapper,
                              StringRedisTemplate stringRedisTemplate,
                              ForceLogoutWebSocketHandler forceLogoutWebSocketHandler) {
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.forceLogoutWebSocketHandler = forceLogoutWebSocketHandler;
    }

    public AuthenticatedPrincipal createSession(Long principalId,
                                                String account,
                                                IdentityType identityType,
                                                String nickname,
                                                String avatar,
                                                String roleCode,
                                                List<String> permissions) {
        StpUtil.login(identityType.code() + ":" + principalId);
        StpUtil.getSession().set(SESSION_KEY_ACCOUNT, account);
        StpUtil.getSession().set(SESSION_KEY_PRINCIPAL_ID, principalId);
        StpUtil.getSession().set(SESSION_KEY_IDENTITY_TYPE, identityType.code());
        StpUtil.getSession().set(SESSION_KEY_NICKNAME, nickname);
        StpUtil.getSession().set(SESSION_KEY_AVATAR, avatar);
        StpUtil.getSession().set(SESSION_KEY_ROLE_CODE, roleCode);
        StpUtil.getSession().set(SESSION_KEY_PERMISSIONS, writePermissions(permissions));
        return currentPrincipal();
    }

    public void refreshAdminSession(String nickname, String roleCode, List<String> permissions) {
        StpUtil.checkLogin();
        applyAdminSession(StpUtil.getSession(), nickname, roleCode, permissions);
    }

    public void refreshAdminSessionByAdminId(Long adminId, String nickname, String roleCode, List<String> permissions) {
        if (adminId == null) {
            return;
        }
        String loginId = IdentityType.ADMIN.code() + ":" + adminId;
        try {
            applyAdminSession(StpUtil.getSessionByLoginId(loginId), nickname, roleCode, permissions);
        } catch (Exception ignored) {
            // The target admin may be offline or the session may have expired; database state is still authoritative for next login.
        }
    }

    private void applyAdminSession(SaSession session, String nickname, String roleCode, List<String> permissions) {
        if (session == null) {
            return;
        }
        if (nickname != null) {
            session.set(SESSION_KEY_NICKNAME, nickname);
        }
        if (roleCode != null) {
            session.set(SESSION_KEY_ROLE_CODE, roleCode);
        }
        if (permissions != null) {
            session.set(SESSION_KEY_PERMISSIONS, writePermissions(permissions));
        }
    }

    public AuthenticatedPrincipal currentPrincipal() {
        StpUtil.checkLogin();
        Object principalId = StpUtil.getSession().get(SESSION_KEY_PRINCIPAL_ID);
        return new AuthenticatedPrincipal(
                StpUtil.getLoginIdAsString(),
                principalId == null ? null : Long.parseLong(String.valueOf(principalId)),
                StpUtil.getSession().getString(SESSION_KEY_ACCOUNT),
                IdentityType.fromCode(StpUtil.getSession().getString(SESSION_KEY_IDENTITY_TYPE)),
                StpUtil.getSession().getString(SESSION_KEY_NICKNAME),
                StpUtil.getSession().getString(SESSION_KEY_AVATAR),
                StpUtil.getTokenValue(),
                StpUtil.getSession().getString(SESSION_KEY_ROLE_CODE),
                readPermissions(StpUtil.getSession().getString(SESSION_KEY_PERMISSIONS))
        );
    }

    public void disableUserSession(Long userId) {
        if (userId == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(disabledUserBlacklistKey(userId), "1");
        forceLogoutWebSocketHandler.forceLogout(userId, "您的账号已被禁用，即将退出登录。您可以继续以游客身份浏览，或联系客服咨询。");
        StpUtil.logout(IdentityType.USER.code() + ":" + userId);
    }

    public void disableAdminSession(Long adminId) {
        if (adminId == null) {
            return;
        }
        forceLogoutWebSocketHandler.forceLogoutAdmin(adminId, "您的后台账号已被禁用，即将退出登录。如需恢复访问，请联系超级管理员。");
        StpUtil.logout(IdentityType.ADMIN.code() + ":" + adminId);
    }

    public void enableUserSession(Long userId) {
        if (userId == null) {
            return;
        }
        stringRedisTemplate.delete(disabledUserBlacklistKey(userId));
    }

    public boolean isUserDisabled(Long userId) {
        return userId != null && Boolean.TRUE.equals(stringRedisTemplate.hasKey(disabledUserBlacklistKey(userId)));
    }

    public void logout() {
        StpUtil.checkLogin();
        StpUtil.logout();
    }

    private String disabledUserBlacklistKey(Long userId) {
        return DISABLED_USER_BLACKLIST_KEY_PREFIX + userId;
    }

    private String writePermissions(List<String> permissions) {
        try {
            return objectMapper.writeValueAsString(permissions == null ? Collections.emptyList() : permissions);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> readPermissions(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
