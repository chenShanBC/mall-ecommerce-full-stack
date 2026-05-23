package com.mallfei.admin.domain.service;

import com.mallfei.admin.domain.model.AdminAccount;
import com.mallfei.admin.domain.model.AdminPermissionCatalog;
import com.mallfei.admin.domain.repository.AdminAccountRepository;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.common.security.PasswordCodec;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminDomainService {

    private final AdminAccountRepository adminAccountRepository;
    private final PasswordCodec passwordCodec;

    public AdminDomainService(AdminAccountRepository adminAccountRepository, PasswordCodec passwordCodec) {
        this.adminAccountRepository = adminAccountRepository;
        this.passwordCodec = passwordCodec;
    }

    public AdminAccount loadByUsername(String username) {
        AdminAccount account = adminAccountRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.badRequest("管理员账号不存在"));
        return account.superAdmin() ? account.resetPermissions("SUPER_ADMIN", AdminPermissionCatalog.superAdminPermissions()) : account;
    }

    public AdminAccount loadById(Long id) {
        AdminAccount account = adminAccountRepository.findById(id)
                .orElseThrow(() -> BusinessException.badRequest("管理员不存在"));
        return account.superAdmin() ? account.resetPermissions("SUPER_ADMIN", AdminPermissionCatalog.superAdminPermissions()) : account;
    }

    public void validateLogin(AdminAccount adminAccount, String rawPassword) {
        if (!adminAccount.enabled()) {
            throw BusinessException.forbidden("当前管理员已被禁用");
        }
        if (!passwordCodec.matches(rawPassword, adminAccount.passwordHash())) {
            throw BusinessException.badRequest("密码错误");
        }
    }

    public void validateOldPassword(AdminAccount adminAccount, String rawPassword) {
        if (!passwordCodec.matches(rawPassword, adminAccount.passwordHash())) {
            throw BusinessException.badRequest("原密码错误");
        }
    }

    public String encodePassword(String rawPassword) {
        return passwordCodec.encode(rawPassword);
    }

    public AdminAccount createAccount(Long userId,
                                      String username,
                                      String rawPassword,
                                      String nickname,
                                      String roleCode,
                                      List<String> permissions) {
        if (adminAccountRepository.existsByUsername(username)) {
            throw BusinessException.badRequest("运营账号已存在");
        }
        List<String> finalPermissions = resolveAssignablePermissions(roleCode, permissions);
        return AdminAccount.create(userId, username, passwordCodec.encode(rawPassword), nickname, AdminPermissionCatalog.normalizeRoleCode(roleCode), finalPermissions);
    }

    public AdminAccount resetPermissions(AdminAccount account, String roleCode, List<String> permissions) {
        List<String> finalPermissions = resolveAssignablePermissions(roleCode, permissions);
        return account.resetPermissions(AdminPermissionCatalog.normalizeRoleCode(roleCode), finalPermissions);
    }

    private List<String> resolveAssignablePermissions(String roleCode, List<String> permissions) {
        if (roleCode == null || roleCode.isBlank()) {
            throw BusinessException.badRequest("角色编码不能为空");
        }
        String normalizedRoleCode = AdminPermissionCatalog.normalizeRoleCode(roleCode);
        if (normalizedRoleCode.equalsIgnoreCase("SUPER_ADMIN")) {
            return AdminPermissionCatalog.superAdminPermissions();
        }
        List<String> finalPermissions = permissions == null || permissions.isEmpty()
                ? AdminPermissionCatalog.defaultPermissions(normalizedRoleCode)
                : permissions;
        AdminPermissionCatalog.validateAssignable(normalizedRoleCode, finalPermissions);
        return finalPermissions;
    }
}
