package com.mallfei.admin.domain.model;

import java.util.List;
import java.util.Map;

public final class AdminPermissionCatalog {

    public static final String SYSTEM_ACCOUNT_MANAGE = "system:account:manage";
    public static final String SYSTEM_LOG_VIEW = "system:log:view";
    public static final String USER_VIEW = "user:view";
    public static final String USER_MANAGE = "user:manage";
    public static final String PRODUCT_VIEW = "product:view";
    public static final String PRODUCT_MANAGE = "product:manage";
    public static final String ORDER_VIEW = "order:view";
    public static final String ORDER_MANAGE = "order:manage";
    public static final String STOCK_VIEW = "stock:view";
    public static final String STOCK_MANAGE = "stock:manage";
    public static final String PAY_VIEW = "pay:view";
    public static final String PAY_MANAGE = "pay:manage";
    public static final String RECONCILE_VIEW = "reconcile:view";
    public static final String RECONCILE_MANAGE = "reconcile:manage";

    private static final List<AdminRole> BUILT_IN_ROLES = List.of(
            new AdminRole("SUPER_ADMIN", "超级管理员", List.of(SYSTEM_ACCOUNT_MANAGE, SYSTEM_LOG_VIEW, USER_VIEW, USER_MANAGE, PRODUCT_VIEW, PRODUCT_MANAGE, ORDER_VIEW, ORDER_MANAGE, STOCK_VIEW, STOCK_MANAGE, PAY_VIEW, PAY_MANAGE, RECONCILE_VIEW, RECONCILE_MANAGE), true),
            new AdminRole("PRODUCT_OPERATOR", "商品运营", List.of(PRODUCT_VIEW, PRODUCT_MANAGE), true),
            new AdminRole("ORDER_OPERATOR", "订单运营", List.of(ORDER_VIEW, ORDER_MANAGE, PAY_VIEW, RECONCILE_VIEW), true),
            new AdminRole("STOCK_OPERATOR", "库存运营", List.of(STOCK_VIEW, STOCK_MANAGE), true),
            new AdminRole("FINANCE_OPERATOR", "支付财务", List.of(PAY_VIEW, PAY_MANAGE, RECONCILE_VIEW, RECONCILE_MANAGE), true),
            new AdminRole("AUDIT_OPERATOR", "审计运营", List.of(SYSTEM_LOG_VIEW, USER_VIEW, PRODUCT_VIEW, ORDER_VIEW, STOCK_VIEW, PAY_VIEW, RECONCILE_VIEW), true),
            new AdminRole("USER_OPERATOR", "用户运营", List.of(USER_VIEW, USER_MANAGE, ORDER_VIEW), true)
    );

    private AdminPermissionCatalog() {
    }

    public static List<AdminRole> builtInRoles() {
        return BUILT_IN_ROLES;
    }

    public static List<String> defaultPermissions(String roleCode) {
        return builtInRoles().stream().filter(role -> role.code().equalsIgnoreCase(roleCode)).findFirst().map(AdminRole::defaultPermissions).orElse(List.of(PRODUCT_VIEW));
    }

    public static List<String> superAdminPermissions() {
        return defaultPermissions("SUPER_ADMIN");
    }

    public static Map<String, List<String>> rolePermissionTemplates() {
        return builtInRoles().stream().collect(java.util.stream.Collectors.toMap(AdminRole::code, AdminRole::defaultPermissions, (left, right) -> left, java.util.LinkedHashMap::new));
    }
}
