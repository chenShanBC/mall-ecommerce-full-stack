package com.mallfei.auth.infrastructure.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.enums.IdentityType;
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
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public AuthSessionManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
        if (nickname != null) {
          StpUtil.getSession().set(SESSION_KEY_NICKNAME, nickname);
        }
        if (roleCode != null) {
          StpUtil.getSession().set(SESSION_KEY_ROLE_CODE, roleCode);
        }
        if (permissions != null) {
          StpUtil.getSession().set(SESSION_KEY_PERMISSIONS, writePermissions(permissions));
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

    public void logout() {
        StpUtil.checkLogin();
        StpUtil.logout();
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
