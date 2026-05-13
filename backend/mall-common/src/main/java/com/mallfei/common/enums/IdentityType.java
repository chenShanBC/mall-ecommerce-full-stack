package com.mallfei.common.enums;

public enum IdentityType {
    USER,
    ADMIN;

    public static IdentityType fromCode(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }
        return "ADMIN".equalsIgnoreCase(value) || "admin".equalsIgnoreCase(value) ? ADMIN : USER;
    }

    public String code() {
        return name();
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }
}
