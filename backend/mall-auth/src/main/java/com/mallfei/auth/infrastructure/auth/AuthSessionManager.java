package com.mallfei.auth.infrastructure.auth;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallfei.auth.domain.model.AuthDeviceType;
import com.mallfei.auth.domain.model.AuthSessionEventLog;
import com.mallfei.auth.domain.model.LoginSessionCommand;
import com.mallfei.auth.domain.model.SessionConcurrencyPolicy;
import com.mallfei.auth.domain.service.AuthSessionEventLogService;
import com.mallfei.auth.domain.service.AuthSessionPolicyService;
import com.mallfei.auth.infrastructure.websocket.ForceLogoutWebSocketHandler;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.enums.IdentityType;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

@Component
public class AuthSessionManager {

    private static final Logger log = LoggerFactory.getLogger(AuthSessionManager.class);

    private static final String SESSION_KEY_ACCOUNT = "account";
    private static final String SESSION_KEY_PRINCIPAL_ID = "principalId";
    private static final String SESSION_KEY_IDENTITY_TYPE = "identityType";
    private static final String SESSION_KEY_NICKNAME = "nickname";
    private static final String SESSION_KEY_AVATAR = "avatar";
    private static final String SESSION_KEY_ROLE_CODE = "roleCode";
    private static final String SESSION_KEY_PERMISSIONS = "permissions";
    private static final String SESSION_KEY_DEVICE_TYPE = "deviceType";
    private static final String DISABLED_USER_BLACKLIST_KEY_PREFIX = "auth:user:disabled:";
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ForceLogoutWebSocketHandler forceLogoutWebSocketHandler;
    private final AuthSessionPolicyService authSessionPolicyService;
    private final AuthSessionEventLogService authSessionEventLogService;

    public AuthSessionManager(ObjectMapper objectMapper,
                              StringRedisTemplate stringRedisTemplate,
                              ForceLogoutWebSocketHandler forceLogoutWebSocketHandler,
                              AuthSessionPolicyService authSessionPolicyService,
                              AuthSessionEventLogService authSessionEventLogService) {
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.forceLogoutWebSocketHandler = forceLogoutWebSocketHandler;
        this.authSessionPolicyService = authSessionPolicyService;
        this.authSessionEventLogService = authSessionEventLogService;
    }

    public AuthenticatedPrincipal createSession(LoginSessionCommand command) {
        SessionConcurrencyPolicy policy = authSessionPolicyService.concurrencyPolicy(command.identityType(), command.deviceType());
        SaLoginModel loginModel = new SaLoginModel()
                .setDevice(command.deviceType().code());
        if (policy.enabled() && policy.kickPreviousLogin()) {
            loginModel.setIsLastingCookie(false);
        }
        StpUtil.login(command.loginId(), loginModel);
        StpUtil.getSession().set(SESSION_KEY_ACCOUNT, command.account());
        StpUtil.getSession().set(SESSION_KEY_PRINCIPAL_ID, command.principalId());
        StpUtil.getSession().set(SESSION_KEY_IDENTITY_TYPE, command.identityType().code());
        StpUtil.getSession().set(SESSION_KEY_NICKNAME, command.nickname());
        StpUtil.getSession().set(SESSION_KEY_AVATAR, command.avatar());
        StpUtil.getSession().set(SESSION_KEY_ROLE_CODE, command.roleCode());
        StpUtil.getSession().set(SESSION_KEY_PERMISSIONS, writePermissions(command.permissions()));
        StpUtil.getSession().set(SESSION_KEY_DEVICE_TYPE, command.deviceType().code());
        String ip = clientIp();
        String userAgent = safeUserAgent();
        notifyPreviousSessionIfNecessary(command, policy, ip, userAgent);
        log.info("Login session created, loginId={}, identityType={}, deviceType={}, singleLogin={}, ip={}, userAgent={}",
                command.loginId(), command.identityType().code(), command.deviceType().code(), policy.enabled(), ip, userAgent);
        authSessionEventLogService.appendSafely(AuthSessionEventLog.success(
                command,
                tokenDigest(StpUtil.getTokenValue()),
                ip,
                userAgent,
                policy.enabled() ? "账号登录成功，已启用同端单登录策略" : "账号登录成功"
        ));
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
        String message = "您的账号已被禁用，即将退出登录。您可以继续以游客身份浏览，或联系客服咨询。";
        forceLogoutWebSocketHandler.forceLogout(userId, message);
        authSessionEventLogService.appendSafely(AuthSessionEventLog.disabledForceLogout(
                userId,
                IdentityType.USER,
                AuthDeviceType.USER_H5,
                IdentityType.USER.code() + ":" + userId,
                clientIp(),
                safeUserAgent(),
                message
        ));
        StpUtil.logout(IdentityType.USER.code() + ":" + userId);
    }

    public void disableAdminSession(Long adminId) {
        if (adminId == null) {
            return;
        }
        String message = "您的后台账号已被禁用，即将退出登录。如需恢复访问，请联系超级管理员。";
        forceLogoutWebSocketHandler.forceLogoutAdmin(adminId, message);
        authSessionEventLogService.appendSafely(AuthSessionEventLog.disabledForceLogout(
                adminId,
                IdentityType.ADMIN,
                AuthDeviceType.ADMIN_WEB,
                IdentityType.ADMIN.code() + ":" + adminId,
                clientIp(),
                safeUserAgent(),
                message
        ));
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
        SaSession session = StpUtil.getSession();
        String ip = clientIp();
        String userAgent = safeUserAgent();
        String loginId = StpUtil.getLoginIdAsString();
        String deviceTypeCode = session.getString(SESSION_KEY_DEVICE_TYPE);
        log.info("Login session logout, loginId={}, deviceType={}, ip={}", loginId, deviceTypeCode, ip);
        authSessionEventLogService.appendSafely(AuthSessionEventLog.logout(
                Long.parseLong(String.valueOf(session.get(SESSION_KEY_PRINCIPAL_ID))),
                IdentityType.fromCode(session.getString(SESSION_KEY_IDENTITY_TYPE)),
                session.getString(SESSION_KEY_ACCOUNT),
                AuthDeviceType.fromNullable(deviceTypeCode, IdentityType.fromCode(session.getString(SESSION_KEY_IDENTITY_TYPE))),
                loginId,
                tokenDigest(StpUtil.getTokenValue()),
                ip,
                userAgent
        ));
        StpUtil.logout();
    }

    private void notifyPreviousSessionIfNecessary(LoginSessionCommand command, SessionConcurrencyPolicy policy, String ip, String userAgent) {
        if (!policy.enabled() || !policy.kickPreviousLogin()) {
            return;
        }
        String message = command.identityType().isAdmin()
                ? "您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。"
                : "您的账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码。";
        if (command.identityType().isAdmin()) {
            forceLogoutWebSocketHandler.forceLogoutAdmin(command.principalId(), message);
        } else {
            forceLogoutWebSocketHandler.forceLogout(command.principalId(), message);
        }
        authSessionEventLogService.appendSafely(AuthSessionEventLog.replaced(command, ip, userAgent, message));
        log.info("Previous login session notified, loginId={}, identityType={}, deviceType={}, reason=LOGIN_REPLACED",
                command.loginId(), command.identityType().code(), command.deviceType().code());
    }

    private String clientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "unknown";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String safeUserAgent() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "unknown";
        }
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return "unknown";
        }
        return userAgent.length() > 180 ? userAgent.substring(0, 180) : userAgent;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String tokenDigest(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            return null;
        }
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
