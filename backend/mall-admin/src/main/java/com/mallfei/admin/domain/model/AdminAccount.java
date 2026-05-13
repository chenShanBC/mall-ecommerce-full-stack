package com.mallfei.admin.domain.model;

import com.mallfei.common.exception.BusinessException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record AdminAccount(
        Long id,
        Long userId,
        String username,
        String passwordHash,
        String nickname,
        String roleCode,
        String status,
        List<String> permissions
) {

    public static AdminAccount create(Long userId,
                                      String username,
                                      String passwordHash,
                                      String nickname,
                                      String roleCode,
                                      List<String> permissions) {
        return new AdminAccount(null, userId, username, passwordHash, nickname, roleCode, "ENABLED", normalizePermissions(permissions));
    }

    public boolean enabled() {
        return "ENABLED".equalsIgnoreCase(status);
    }

    public boolean superAdmin() {
        return "SUPER_ADMIN".equalsIgnoreCase(roleCode);
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    public AdminAccount disable() {
        if (superAdmin()) {
            throw BusinessException.badRequest("超级管理员不可被禁用");
        }
        if (!enabled()) {
            return this;
        }
        return new AdminAccount(id, userId, username, passwordHash, nickname, roleCode, "DISABLED", permissions);
    }

    public AdminAccount enable() {
        if (enabled()) {
            return this;
        }
        return new AdminAccount(id, userId, username, passwordHash, nickname, roleCode, "ENABLED", permissions);
    }

    public AdminAccount rename(String newNickname) {
        if (newNickname == null || newNickname.isBlank()) {
            throw BusinessException.badRequest("昵称不能为空");
        }
        if (Objects.equals(nickname, newNickname.trim())) {
            return this;
        }
        return new AdminAccount(id, userId, username, passwordHash, newNickname.trim(), roleCode, status, permissions);
    }

    public AdminAccount changePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw BusinessException.badRequest("密码不能为空");
        }
        return new AdminAccount(id, userId, username, newPasswordHash, nickname, roleCode, status, permissions);
    }

    public AdminAccount resetPermissions(String roleCode, List<String> permissions) {
        if (superAdmin()) {
            return new AdminAccount(id, userId, username, passwordHash, nickname, "SUPER_ADMIN", status, AdminPermissionCatalog.superAdminPermissions());
        }
        return new AdminAccount(id, userId, username, passwordHash, nickname, roleCode, status, normalizePermissions(permissions));
    }

    private static List<String> normalizePermissions(List<String> permissions) {
        Set<String> normalized = new LinkedHashSet<>(permissions == null ? List.of() : permissions);
        if (normalized.isEmpty()) {
            throw BusinessException.badRequest("权限列表不能为空");
        }
        return new ArrayList<>(normalized);
    }
}
