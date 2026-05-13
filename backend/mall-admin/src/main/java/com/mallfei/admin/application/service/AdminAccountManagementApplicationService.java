package com.mallfei.admin.application.service;

import com.mallfei.admin.application.dto.AdminAssignRoleRequest;
import com.mallfei.admin.application.dto.AdminCreateAccountRequest;
import com.mallfei.admin.application.dto.AdminUpdateAccountPermissionRequest;
import com.mallfei.admin.application.vo.AdminAccountView;
import com.mallfei.admin.application.vo.AdminOperationLogView;
import com.mallfei.admin.application.vo.AdminRoleView;
import com.mallfei.admin.domain.model.AdminAccount;
import com.mallfei.admin.domain.model.AdminOperationLog;
import com.mallfei.admin.domain.model.AdminPermissionCatalog;
import com.mallfei.admin.domain.model.AdminRole;
import com.mallfei.admin.domain.repository.AdminAccountRepository;
import com.mallfei.admin.domain.repository.AdminOperationLogRepository;
import com.mallfei.admin.domain.service.AdminDomainService;
import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.common.api.PageResult;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class AdminAccountManagementApplicationService {

    private final AuthFacade authFacade;
    private final AdminDomainService adminDomainService;
    private final AdminAccountRepository adminAccountRepository;
    private final AdminOperationLogRepository adminOperationLogRepository;

    public AdminAccountManagementApplicationService(AuthFacade authFacade, AdminDomainService adminDomainService, AdminAccountRepository adminAccountRepository, AdminOperationLogRepository adminOperationLogRepository) {
        this.authFacade = authFacade;
        this.adminDomainService = adminDomainService;
        this.adminAccountRepository = adminAccountRepository;
        this.adminOperationLogRepository = adminOperationLogRepository;
    }

    public PageResult<AdminAccountView> listAccounts(String keyword, String roleCode, String status, long page, long size) {
        requireSuperAdmin();
        PageResult<AdminAccount> result = adminAccountRepository.search(keyword, roleCode, status, page, size);
        return new PageResult<>(result.page(), result.size(), result.total(), result.pages(), result.records().stream().map(this::toView).toList());
    }
    public Map<String, List<String>> permissionTemplates() { requireSuperAdmin(); return AdminPermissionCatalog.rolePermissionTemplates(); }
    public List<AdminRoleView> roles() { requireSuperAdmin(); return AdminPermissionCatalog.builtInRoles().stream().map(this::toRoleView).toList(); }

    public AdminAccountView createAccount(AdminCreateAccountRequest request) {
        AdminAccount operator = requireSuperAdmin();
        AdminAccount created = adminAccountRepository.save(adminDomainService.createAccount(request.userId(), request.username(), request.password(), request.nickname(), request.roleCode(), request.permissions()));
        writeLog(operator, "SYSTEM", "ACCOUNT_CREATE", "创建运营账号：" + created.username() + "，角色=" + created.roleCode(), "SUCCESS");
        return toView(created);
    }

    public AdminAccountView disableAccount(Long adminId) {
        AdminAccount operator = requireSuperAdmin();
        AdminAccount updated = adminAccountRepository.update(adminDomainService.loadById(adminId).disable());
        writeLog(operator, "SYSTEM", "ACCOUNT_DISABLE", "禁用运营账号：" + updated.username(), "SUCCESS");
        return toView(updated);
    }

    public AdminAccountView enableAccount(Long adminId) {
        AdminAccount operator = requireSuperAdmin();
        AdminAccount updated = adminAccountRepository.update(adminDomainService.loadById(adminId).enable());
        writeLog(operator, "SYSTEM", "ACCOUNT_ENABLE", "启用运营账号：" + updated.username(), "SUCCESS");
        return toView(updated);
    }

    public AdminAccountView assignRole(Long adminId, AdminAssignRoleRequest request) {
        AdminAccount operator = requireSuperAdmin();
        AdminAccount target = adminDomainService.loadById(adminId);
        List<String> permissions = Boolean.TRUE.equals(request.useDefaultPermissions()) ? AdminPermissionCatalog.defaultPermissions(request.roleCode()) : target.permissions();
        AdminAccount updated = adminAccountRepository.update(adminDomainService.resetPermissions(target, request.roleCode(), permissions));
        writeLog(operator, "SYSTEM", "ACCOUNT_ROLE_ASSIGN", "分配运营角色：" + updated.username() + " -> " + updated.roleCode(), "SUCCESS");
        return toView(updated);
    }

    public AdminAccountView updatePermissions(Long adminId, AdminUpdateAccountPermissionRequest request) {
        AdminAccount operator = requireSuperAdmin();
        AdminAccount target = adminDomainService.loadById(adminId);
        AdminAccount updated = adminAccountRepository.update(adminDomainService.resetPermissions(target, request.roleCode(), request.permissions()));
        if (operator.id().equals(updated.id())) {
            authFacade.refreshAdminSession(updated.nickname(), updated.roleCode(), updated.permissions());
        }
        writeLog(operator, "SYSTEM", "ACCOUNT_PERMISSION_UPDATE", "更新运营账号权限：" + updated.username() + "，角色=" + updated.roleCode(), "SUCCESS");
        return toView(updated);
    }

    public PageResult<AdminOperationLogView> operationLogs(String keyword, String module, String result, long page, long size, String sortBy, String sortOrder) {
        requireSuperAdmin();
        List<AdminOperationLogView> rows = adminOperationLogRepository.findAll().stream()
                .map(this::toLogView)
                .filter(log -> blank(module) || module.equals(log.operationModule()))
                .filter(log -> blank(result) || result.equals(log.operationResult()))
                .filter(log -> blank(keyword) || contains(log.operatorUsername(), keyword) || contains(log.operationModule(), keyword) || contains(log.operationType(), keyword) || contains(log.operationContent(), keyword))
                .toList();
        return PageResult.of(sortList(rows, logComparator(sortBy), blank(sortOrder) ? "asc" : sortOrder), page, size);
    }

    public void recordOperation(String module, String type, String content, String result) {
        writeLog(requireAdminOperator(), module, type, content, result);
    }

    private AdminAccount requireAdminOperator() {
        AuthenticatedPrincipal principal = authFacade.currentPrincipal();
        if (principal == null || !principal.isAdmin()) throw BusinessException.forbidden("仅管理员可访问当前接口");
        return adminDomainService.loadById(principal.principalId());
    }

    private AdminAccount requireSuperAdmin() {
        AdminAccount operator = requireAdminOperator();
        if (!operator.superAdmin()) throw BusinessException.forbidden("仅超级管理员可执行当前操作");
        return operator;
    }

    private void writeLog(AdminAccount operator, String module, String type, String content, String result) { adminOperationLogRepository.save(new AdminOperationLog(null, operator.id(), operator.username(), module, type, content, result, LocalDateTime.now())); }
    private Comparator<AdminOperationLogView> logComparator(String sortBy) {
        if (blank(sortBy)) return Comparator.comparing(AdminOperationLogView::id, Comparator.nullsLast(Long::compareTo));
        return switch (sortBy) {
            case "id" -> Comparator.comparing(AdminOperationLogView::id, Comparator.nullsLast(Long::compareTo));
            case "operatorUsername" -> Comparator.comparing(AdminOperationLogView::operatorUsername, Comparator.nullsLast(String::compareTo));
            case "operationModule" -> Comparator.comparing(AdminOperationLogView::operationModule, Comparator.nullsLast(String::compareTo));
            case "operationType" -> Comparator.comparing(AdminOperationLogView::operationType, Comparator.nullsLast(String::compareTo));
            case "operationResult" -> Comparator.comparing(AdminOperationLogView::operationResult, Comparator.nullsLast(String::compareTo));
            case "createdAt" -> Comparator.comparing(AdminOperationLogView::createdAt, Comparator.nullsLast(LocalDateTime::compareTo));
            default -> Comparator.comparing(AdminOperationLogView::id, Comparator.nullsLast(Long::compareTo));
        };
    }
    private <T> List<T> sortList(List<T> rows, Comparator<T> comparator, String sortOrder) { if (comparator == null) return rows; Comparator<T> actual = "asc".equalsIgnoreCase(sortOrder) ? comparator : comparator.reversed(); return rows.stream().sorted(actual).toList(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private boolean contains(String source, String keyword) { return source != null && source.toLowerCase().contains(keyword.toLowerCase().trim()); }
    private AdminAccountView toView(AdminAccount account) { return new AdminAccountView(account.id(), account.userId(), account.username(), account.nickname(), account.roleCode(), account.status(), account.permissions(), null); }
    private AdminOperationLogView toLogView(AdminOperationLog log) { return new AdminOperationLogView(log.id(), log.operatorAdminId(), log.operatorUsername(), log.operationModule(), log.operationType(), log.operationContent(), log.operationResult(), log.createdAt()); }
    private AdminRoleView toRoleView(AdminRole role) { return new AdminRoleView(role.code(), role.name(), role.defaultPermissions(), role.builtIn()); }
}
