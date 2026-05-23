package com.mallfei.admin.domain.model;

import com.mallfei.common.exception.BusinessException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AdminPermissionCatalog {

    public static final String DASHBOARD_VIEW = "dashboard:view";
    public static final String ADMIN_VIEW = "admin:view";
    public static final String ADMIN_CREATE = "admin:create";
    public static final String ADMIN_UPDATE = "admin:update";
    public static final String ADMIN_DISABLE = "admin:disable";
    public static final String ROLE_VIEW = "role:view";
    public static final String ROLE_MANAGE = "role:manage";
    public static final String PERMISSION_VIEW = "permission:view";
    public static final String PERMISSION_ASSIGN = "permission:assign";
    public static final String LOG_OPERATION_VIEW = "log:operation:view";
    public static final String USER_VIEW = "user:view";
    public static final String USER_EDIT = "user:edit";
    public static final String USER_DISABLE = "user:disable";
    public static final String PRODUCT_VIEW = "product:view";
    public static final String PRODUCT_CREATE = "product:create";
    public static final String PRODUCT_UPDATE = "product:update";
    public static final String PRODUCT_ON_SALE = "product:on_sale";
    public static final String PRODUCT_OFF_SALE = "product:off_sale";
    public static final String CATEGORY_MANAGE = "category:manage";
    public static final String STOCK_VIEW = "stock:view";
    public static final String STOCK_LOG_VIEW = "stock:log:view";
    public static final String STOCK_ADJUST = "stock:adjust";
    public static final String ORDER_VIEW = "order:view";
    public static final String ORDER_REMARK = "order:remark";
    public static final String ORDER_SHIP = "order:ship";
    public static final String ORDER_CLOSE = "order:close";
    public static final String ORDER_LOG_VIEW = "order:log:view";
    public static final String AFTERSALE_VIEW = "aftersale:view";
    public static final String AFTERSALE_AUDIT = "aftersale:audit";
    public static final String REFUND_VIEW = "refund:view";
    public static final String REFUND_EXECUTE = "refund:execute";
    public static final String FINANCE_VIEW = "finance:view";
    public static final String PAYMENT_VIEW = "payment:view";
    public static final String RECONCILIATION_VIEW = "reconciliation:view";
    public static final String RECONCILIATION_HANDLE = "reconciliation:handle";

    private static final List<String> ALL_PERMISSIONS = List.of(
            DASHBOARD_VIEW, ADMIN_VIEW, ADMIN_CREATE, ADMIN_UPDATE, ADMIN_DISABLE, ROLE_VIEW, ROLE_MANAGE,
            PERMISSION_VIEW, PERMISSION_ASSIGN, LOG_OPERATION_VIEW, USER_VIEW, USER_EDIT, USER_DISABLE,
            PRODUCT_VIEW, PRODUCT_CREATE, PRODUCT_UPDATE, PRODUCT_ON_SALE, PRODUCT_OFF_SALE, CATEGORY_MANAGE,
            STOCK_VIEW, STOCK_LOG_VIEW, STOCK_ADJUST, ORDER_VIEW, ORDER_REMARK, ORDER_SHIP, ORDER_CLOSE,
            ORDER_LOG_VIEW, AFTERSALE_VIEW, AFTERSALE_AUDIT, REFUND_VIEW, REFUND_EXECUTE, FINANCE_VIEW,
            PAYMENT_VIEW, RECONCILIATION_VIEW, RECONCILIATION_HANDLE
    );

    private static final Map<String, List<String>> ROLE_DEFAULT_PERMISSIONS = new LinkedHashMap<>();
    private static final Map<String, List<String>> ROLE_PERMISSION_SCOPES = new LinkedHashMap<>();
    private static final List<AdminRole> BUILT_IN_ROLES;

    static {
        ROLE_DEFAULT_PERMISSIONS.put("SUPER_ADMIN", ALL_PERMISSIONS);
        ROLE_DEFAULT_PERMISSIONS.put("PRODUCT_OPERATOR", List.of(DASHBOARD_VIEW, PRODUCT_VIEW, PRODUCT_CREATE, PRODUCT_UPDATE, PRODUCT_ON_SALE, PRODUCT_OFF_SALE, CATEGORY_MANAGE, STOCK_VIEW, STOCK_LOG_VIEW));
        ROLE_DEFAULT_PERMISSIONS.put("ORDER_OPERATOR", List.of(DASHBOARD_VIEW, ORDER_VIEW, ORDER_REMARK, ORDER_SHIP, ORDER_LOG_VIEW, AFTERSALE_VIEW, AFTERSALE_AUDIT));
        ROLE_DEFAULT_PERMISSIONS.put("STOCK_OPERATOR", List.of(DASHBOARD_VIEW, STOCK_VIEW, STOCK_LOG_VIEW, STOCK_ADJUST, PRODUCT_VIEW));
        ROLE_DEFAULT_PERMISSIONS.put("FINANCE_OPERATOR", List.of(DASHBOARD_VIEW, FINANCE_VIEW, PAYMENT_VIEW, REFUND_VIEW, REFUND_EXECUTE, RECONCILIATION_VIEW, RECONCILIATION_HANDLE, ORDER_VIEW, AFTERSALE_VIEW));
        ROLE_DEFAULT_PERMISSIONS.put("CUSTOMER_SERVICE", List.of(DASHBOARD_VIEW, USER_VIEW, ORDER_VIEW, ORDER_REMARK, ORDER_LOG_VIEW, AFTERSALE_VIEW));
        ROLE_DEFAULT_PERMISSIONS.put("USER_OPERATOR", List.of(DASHBOARD_VIEW, USER_VIEW, USER_EDIT, USER_DISABLE, ORDER_VIEW));
        ROLE_DEFAULT_PERMISSIONS.put("AUDIT_OPERATOR", List.of(DASHBOARD_VIEW, USER_VIEW, PRODUCT_VIEW, STOCK_VIEW, STOCK_LOG_VIEW, ORDER_VIEW, ORDER_LOG_VIEW, AFTERSALE_VIEW, REFUND_VIEW, FINANCE_VIEW, PAYMENT_VIEW, RECONCILIATION_VIEW, ADMIN_VIEW, ROLE_VIEW, PERMISSION_VIEW, LOG_OPERATION_VIEW));

        ROLE_PERMISSION_SCOPES.put("SUPER_ADMIN", ALL_PERMISSIONS);
        ROLE_PERMISSION_SCOPES.put("PRODUCT_OPERATOR", append(ROLE_DEFAULT_PERMISSIONS.get("PRODUCT_OPERATOR"), ORDER_VIEW, AFTERSALE_VIEW, USER_VIEW, LOG_OPERATION_VIEW));
        ROLE_PERMISSION_SCOPES.put("ORDER_OPERATOR", append(ROLE_DEFAULT_PERMISSIONS.get("ORDER_OPERATOR"), USER_VIEW, PRODUCT_VIEW, STOCK_VIEW, PAYMENT_VIEW, REFUND_VIEW, RECONCILIATION_VIEW, LOG_OPERATION_VIEW));
        ROLE_PERMISSION_SCOPES.put("STOCK_OPERATOR", append(ROLE_DEFAULT_PERMISSIONS.get("STOCK_OPERATOR"), PRODUCT_UPDATE, CATEGORY_MANAGE, ORDER_VIEW, ORDER_LOG_VIEW, LOG_OPERATION_VIEW));
        ROLE_PERMISSION_SCOPES.put("FINANCE_OPERATOR", append(ROLE_DEFAULT_PERMISSIONS.get("FINANCE_OPERATOR"), USER_VIEW, ORDER_LOG_VIEW, AFTERSALE_AUDIT, LOG_OPERATION_VIEW));
        ROLE_PERMISSION_SCOPES.put("CUSTOMER_SERVICE", append(ROLE_DEFAULT_PERMISSIONS.get("CUSTOMER_SERVICE"), USER_EDIT, PRODUCT_VIEW, PAYMENT_VIEW, REFUND_VIEW, STOCK_VIEW));
        ROLE_PERMISSION_SCOPES.put("USER_OPERATOR", append(ROLE_DEFAULT_PERMISSIONS.get("USER_OPERATOR"), ORDER_REMARK, ORDER_LOG_VIEW, AFTERSALE_VIEW, PRODUCT_VIEW, LOG_OPERATION_VIEW));
        ROLE_PERMISSION_SCOPES.put("AUDIT_OPERATOR", ROLE_DEFAULT_PERMISSIONS.get("AUDIT_OPERATOR"));

        BUILT_IN_ROLES = List.of(
                new AdminRole("SUPER_ADMIN", "超级管理员", ROLE_DEFAULT_PERMISSIONS.get("SUPER_ADMIN"), true),
                new AdminRole("PRODUCT_OPERATOR", "商品运营", ROLE_DEFAULT_PERMISSIONS.get("PRODUCT_OPERATOR"), true),
                new AdminRole("ORDER_OPERATOR", "订单运营", ROLE_DEFAULT_PERMISSIONS.get("ORDER_OPERATOR"), true),
                new AdminRole("STOCK_OPERATOR", "库存运营", ROLE_DEFAULT_PERMISSIONS.get("STOCK_OPERATOR"), true),
                new AdminRole("FINANCE_OPERATOR", "财务人员", ROLE_DEFAULT_PERMISSIONS.get("FINANCE_OPERATOR"), true),
                new AdminRole("CUSTOMER_SERVICE", "客服人员", ROLE_DEFAULT_PERMISSIONS.get("CUSTOMER_SERVICE"), true),
                new AdminRole("USER_OPERATOR", "用户运营", ROLE_DEFAULT_PERMISSIONS.get("USER_OPERATOR"), true),
                new AdminRole("AUDIT_OPERATOR", "审计人员", ROLE_DEFAULT_PERMISSIONS.get("AUDIT_OPERATOR"), true)
        );
    }

    private AdminPermissionCatalog() {
    }

    public static List<AdminRole> builtInRoles() {
        return BUILT_IN_ROLES;
    }

    public static List<String> defaultPermissions(String roleCode) {
        return ROLE_DEFAULT_PERMISSIONS.getOrDefault(normalizeRoleCode(roleCode), List.of(PRODUCT_VIEW));
    }

    public static List<String> permissionScope(String roleCode) {
        return ROLE_PERMISSION_SCOPES.getOrDefault(normalizeRoleCode(roleCode), defaultPermissions(roleCode));
    }

    public static String normalizeRoleCode(String roleCode) {
        String normalized = roleCode == null ? "" : roleCode.trim().toUpperCase();
        return switch (normalized) {
            case "ADMIN", "SUPER", "SUPERADMIN" -> "SUPER_ADMIN";
            case "PRODUCT" -> "PRODUCT_OPERATOR";
            case "ORDER" -> "ORDER_OPERATOR";
            case "STOCK" -> "STOCK_OPERATOR";
            case "FINANCE", "PAY" -> "FINANCE_OPERATOR";
            case "CUSTOMER", "CUSTOMER_SERVICE_OPERATOR", "SERVICE" -> "CUSTOMER_SERVICE";
            case "USER" -> "USER_OPERATOR";
            case "AUDIT" -> "AUDIT_OPERATOR";
            default -> normalized;
        };
    }

    public static List<String> superAdminPermissions() {
        return defaultPermissions("SUPER_ADMIN");
    }

    public static List<String> allPermissions() {
        return ALL_PERMISSIONS;
    }

    public static Map<String, List<String>> rolePermissionTemplates() {
        return builtInRoles().stream().collect(Collectors.toMap(AdminRole::code, AdminRole::defaultPermissions, (left, right) -> left, LinkedHashMap::new));
    }

    public static void validateAssignable(String roleCode, List<String> permissions) {
        String normalizedRoleCode = normalizeRoleCode(roleCode);
        Set<String> allPermissionSet = new LinkedHashSet<>(ALL_PERMISSIONS);
        List<String> unknownPermissions = normalizePermissions(permissions).stream().filter(permission -> !allPermissionSet.contains(permission)).toList();
        if (!unknownPermissions.isEmpty()) {
            throw BusinessException.badRequest("权限不存在或已禁用：" + String.join(",", unknownPermissions));
        }
        Set<String> scope = new LinkedHashSet<>(permissionScope(normalizedRoleCode));
        List<String> exceededPermissions = normalizePermissions(permissions).stream().filter(permission -> !scope.contains(permission)).toList();
        if (!exceededPermissions.isEmpty()) {
            throw BusinessException.forbidden("超出角色权限上限：" + String.join(",", exceededPermissions));
        }
    }

    private static List<String> normalizePermissions(List<String> permissions) {
        return new LinkedHashSet<>(permissions == null ? List.<String>of() : permissions).stream().toList();
    }

    private static List<String> append(List<String> base, String... permissions) {
        return Stream.concat(base.stream(), Stream.of(permissions)).distinct().toList();
    }
}
