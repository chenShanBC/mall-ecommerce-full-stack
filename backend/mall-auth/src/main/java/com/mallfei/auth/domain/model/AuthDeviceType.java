package com.mallfei.auth.domain.model;

import com.mallfei.common.enums.IdentityType;

import java.util.Locale;

public enum AuthDeviceType {

    USER_WEB("user-web"),
    USER_H5("user-h5"),
    ADMIN_WEB("admin-web");

    private final String code;

    AuthDeviceType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static AuthDeviceType defaultOf(IdentityType identityType) {
        if (identityType != null && identityType.isAdmin()) {
            return ADMIN_WEB;
        }
        return USER_H5;
    }

    public static AuthDeviceType fromNullable(String code, IdentityType identityType) {
        if (code == null || code.isBlank()) {
            return defaultOf(identityType);
        }
        String normalized = code.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        for (AuthDeviceType deviceType : values()) {
            if (deviceType.code.equals(normalized)) {
                return deviceType;
            }
        }
        return defaultOf(identityType);
    }
}
