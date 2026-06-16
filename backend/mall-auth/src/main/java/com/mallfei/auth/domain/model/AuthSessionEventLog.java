package com.mallfei.auth.domain.model;

import com.mallfei.common.enums.IdentityType;

import java.time.LocalDateTime;
import java.util.Objects;

public record AuthSessionEventLog(
        Long id,
        Long principalId,
        IdentityType identityType,
        String account,
        AuthDeviceType deviceType,
        AuthSessionEventType eventType,
        String result,
        String loginId,
        String tokenDigest,
        String ip,
        String userAgent,
        String message,
        LocalDateTime createdAt
) {

    private static final int MAX_ACCOUNT_LENGTH = 100;
    private static final int MAX_TOKEN_DIGEST_LENGTH = 64;
    private static final int MAX_IP_LENGTH = 64;
    private static final int MAX_USER_AGENT_LENGTH = 500;
    private static final int MAX_MESSAGE_LENGTH = 500;

    public AuthSessionEventLog {
        Objects.requireNonNull(principalId, "principalId must not be null");
        Objects.requireNonNull(identityType, "identityType must not be null");
        Objects.requireNonNull(deviceType, "deviceType must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        result = blankToDefault(result, "SUCCESS");
        account = truncate(account, MAX_ACCOUNT_LENGTH);
        tokenDigest = truncate(tokenDigest, MAX_TOKEN_DIGEST_LENGTH);
        ip = truncate(blankToDefault(ip, "unknown"), MAX_IP_LENGTH);
        userAgent = truncate(blankToDefault(userAgent, "unknown"), MAX_USER_AGENT_LENGTH);
        message = truncate(message, MAX_MESSAGE_LENGTH);
        createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public static AuthSessionEventLog success(LoginSessionCommand command,
                                              String tokenDigest,
                                              String ip,
                                              String userAgent,
                                              String message) {
        return new AuthSessionEventLog(
                null,
                command.principalId(),
                command.identityType(),
                command.account(),
                command.deviceType(),
                AuthSessionEventType.LOGIN_SUCCESS,
                "SUCCESS",
                command.loginId(),
                tokenDigest,
                ip,
                userAgent,
                message,
                LocalDateTime.now()
        );
    }

    public static AuthSessionEventLog replaced(LoginSessionCommand command,
                                               String ip,
                                               String userAgent,
                                               String message) {
        return new AuthSessionEventLog(
                null,
                command.principalId(),
                command.identityType(),
                command.account(),
                command.deviceType(),
                AuthSessionEventType.LOGIN_REPLACED,
                "SUCCESS",
                command.loginId(),
                null,
                ip,
                userAgent,
                message,
                LocalDateTime.now()
        );
    }

    public static AuthSessionEventLog logout(Long principalId,
                                             IdentityType identityType,
                                             String account,
                                             AuthDeviceType deviceType,
                                             String loginId,
                                             String tokenDigest,
                                             String ip,
                                             String userAgent) {
        return new AuthSessionEventLog(
                null,
                principalId,
                identityType,
                account,
                deviceType,
                AuthSessionEventType.LOGOUT,
                "SUCCESS",
                loginId,
                tokenDigest,
                ip,
                userAgent,
                "账号主动退出登录",
                LocalDateTime.now()
        );
    }

    public static AuthSessionEventLog disabledForceLogout(Long principalId,
                                                          IdentityType identityType,
                                                          AuthDeviceType deviceType,
                                                          String loginId,
                                                          String ip,
                                                          String userAgent,
                                                          String message) {
        return new AuthSessionEventLog(
                null,
                principalId,
                identityType,
                null,
                deviceType,
                AuthSessionEventType.DISABLED_FORCE_LOGOUT,
                "SUCCESS",
                loginId,
                null,
                ip,
                userAgent,
                message,
                LocalDateTime.now()
        );
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
