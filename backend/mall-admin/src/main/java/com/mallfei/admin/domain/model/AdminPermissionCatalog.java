package com.mallfei.admin.domain.model;

import com.mallfei.common.exception.BusinessException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 后台 RBAC 权限目录。
 *
 * <p>当前内置角色收敛为「超管 / 财务 / 订单运营 / 仓储 / 商品运营」五类，角色仅作为权限包，接口和前端展示统一以权限码判断。</p>
 */
public final class AdminPermissionCatalog {

    public static final String DASHBOARD_VIEW = "dashboard:view";
    public static final String DASHBOARD_OPERATIONS_VIEW = "dashboard:operations:view";
    public static final String DASHBOARD_FINANCE_VIEW = "dashboard:finance:view";
    public static final String DASHBOARD_WAREHOUSE_VIEW = "dashboard:warehouse:view";
    public static final String DASHBOARD_PRODUCTS_VIEW = "dashboard:products:view";

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
    public static final String USER_DETAIL_VIEW = "user:detail:view";
    public static final String USER_ADDRESS_VIEW = "user:address:view";
    public static final String USER_EDIT = "user:edit";
    public static final String USER_DISABLE = "user:disable";
    public static final String USER_EXPORT = "user:export";

    public static final String PRODUCT_VIEW = "product:view";
    public static final String PRODUCT_DETAIL_VIEW = "product:detail:view";
    public static final String PRODUCT_CREATE = "product:create";
    public static final String PRODUCT_UPDATE = "product:update";
    public static final String PRODUCT_ON_SALE = "product:on_sale";
    public static final String PRODUCT_OFF_SALE = "product:off_sale";
    public static final String PRODUCT_STATUS_UPDATE = "product:status:update";
    public static final String PRODUCT_SALES_VIEW = "product:sales:view";
    public static final String PRODUCT_SALES_THRESHOLD_VIEW = "product:sales-threshold:view";
    public static final String PRODUCT_SALES_THRESHOLD_CONFIG = "product:sales-threshold:config";
    public static final String PRODUCT_VIOLATION_HANDLE = "product:violation:handle";
    public static final String CATEGORY_MANAGE = "category:manage";
    public static final String CATEGORY_VIEW = "category:view";
    public static final String CATEGORY_CREATE = "category:create";
    public static final String CATEGORY_UPDATE = "category:update";
    public static final String CATEGORY_STATUS_UPDATE = "category:status:update";
    public static final String CATEGORY_DELETE = "category:delete";

    public static final String STOCK_VIEW = "stock:view";
    public static final String STOCK_LOG_VIEW = "stock:log:view";
    public static final String STOCK_ADJUST = "stock:adjust";
    public static final String STOCK_POLICY_UPDATE = "stock:policy:update";
    public static final String STOCK_WARNING_VIEW = "stock:warning:view";
    public static final String STOCK_WARNING_HANDLE = "stock:warning:handle";
    public static final String STOCK_RECONCILE_VIEW = "stock:reconcile:view";
    public static final String STOCK_RECONCILE_CHECK = "stock:reconcile:check";
    public static final String STOCK_RECONCILE_REPAIR = "stock:reconcile:repair";

    public static final String ORDER_VIEW = "order:view";
    public static final String ORDER_DETAIL_VIEW = "order:detail:view";
    public static final String ORDER_REMARK = "order:remark";
    public static final String ORDER_RECEIVER_UPDATE = "order:receiver:update";
    public static final String ORDER_SHIP = "order:ship";
    public static final String ORDER_CLOSE = "order:close";
    public static final String ORDER_EXCEPTION_VIEW = "order:exception:view";
    public static final String ORDER_EXCEPTION_HANDLE = "order:exception:handle";
    public static final String ORDER_PAYMENT_EXCEPTION_HANDLE = "order:payment-exception:handle";
    public static final String ORDER_CONFIRM_PAID = "order:confirm-paid";
    public static final String ORDER_SKU_SWITCH = "order:sku:switch";
    public static final String ORDER_LOG_VIEW = "order:log:view";

    public static final String AFTERSALE_VIEW = "aftersale:view";
    public static final String AFTERSALE_DETAIL_VIEW = "aftersale:detail:view";
    public static final String AFTERSALE_AUDIT = "aftersale:audit";
    public static final String AFTERSALE_REVIEW = "aftersale:review";
    public static final String AFTERSALE_REFUND_VIEW = "aftersale:refund:view";

    public static final String REFUND_VIEW = "refund:view";
    public static final String REFUND_EXECUTE = "refund:execute";
    public static final String REFUND_SYNC = "refund:sync";
    public static final String FINANCE_VIEW = "finance:view";
    public static final String PAYMENT_VIEW = "payment:view";
    public static final String PAYMENT_DETAIL_VIEW = "payment:detail:view";
    public static final String PAYMENT_CLOSE = "payment:close";
    public static final String PAYMENT_SYNC = "payment:sync";
    public static final String PAYMENT_REPAIR = "payment:repair";
    public static final String PAYMENT_CALLBACK_VIEW = "payment:callback:view";

    public static final String RECONCILIATION_VIEW = "reconciliation:view";
    public static final String RECONCILIATION_TASK_CREATE = "reconciliation:task:create";
    public static final String RECONCILIATION_TASK_RUN = "reconciliation:task:run";
    public static final String RECONCILIATION_TASK_ARCHIVE = "reconciliation:task:archive";
    public static final String RECONCILIATION_BILL_IMPORT = "reconciliation:bill:import";
    public static final String RECONCILIATION_DIFF_HANDLE = "reconciliation:diff:handle";
    public static final String RECONCILIATION_DIFF_REPAIR = "reconciliation:diff:repair";
    public static final String RECONCILIATION_HANGING_FOLLOW = "reconciliation:hanging:follow";
    public static final String RECONCILIATION_HANDLE = "reconciliation:handle";

    private static final List<AdminPermissionDefinition> PERMISSION_DEFINITIONS = List.of(
            permission(DASHBOARD_VIEW, "仪表盘访问", "dashboard", "仪表盘", false),
            permission(DASHBOARD_OPERATIONS_VIEW, "运营视角看板", "dashboard", "仪表盘", false),
            permission(DASHBOARD_FINANCE_VIEW, "财务视角看板", "dashboard", "仪表盘", false),
            permission(DASHBOARD_WAREHOUSE_VIEW, "仓储视角看板", "dashboard", "仪表盘", false),
            permission(DASHBOARD_PRODUCTS_VIEW, "商品视角看板", "dashboard", "仪表盘", false),

            permission(ADMIN_VIEW, "账号查看", "system", "系统权限", true),
            permission(ADMIN_CREATE, "账号创建", "system", "系统权限", true),
            permission(ADMIN_UPDATE, "账号更新", "system", "系统权限", true),
            permission(ADMIN_DISABLE, "账号启停", "system", "系统权限", true),
            permission(ROLE_VIEW, "角色查看", "system", "系统权限", true),
            permission(ROLE_MANAGE, "角色管理", "system", "系统权限", true),
            permission(PERMISSION_VIEW, "权限查看", "system", "系统权限", true),
            permission(PERMISSION_ASSIGN, "权限分配", "system", "系统权限", true),
            permission(LOG_OPERATION_VIEW, "操作日志查看", "system", "系统权限", true),

            permission(USER_VIEW, "用户列表查看", "user", "用户管理", false),
            permission(USER_DETAIL_VIEW, "用户详情查看", "user", "用户管理", false),
            permission(USER_ADDRESS_VIEW, "用户地址查看", "user", "用户管理", false),
            permission(USER_EDIT, "用户资料编辑", "user", "用户管理", true),
            permission(USER_DISABLE, "用户启停", "user", "用户管理", true),
            permission(USER_EXPORT, "用户数据导出", "user", "用户管理", true),

            permission(PRODUCT_VIEW, "商品查看", "product", "商品管理", false),
            permission(PRODUCT_DETAIL_VIEW, "商品详情查看", "product", "商品管理", false),
            permission(PRODUCT_CREATE, "商品创建", "product", "商品管理", false),
            permission(PRODUCT_UPDATE, "商品更新", "product", "商品管理", false),
            permission(PRODUCT_ON_SALE, "商品上架", "product", "商品管理", false),
            permission(PRODUCT_OFF_SALE, "商品下架", "product", "商品管理", false),
            permission(PRODUCT_STATUS_UPDATE, "商品状态调整", "product", "商品管理", false),
            permission(PRODUCT_SALES_VIEW, "商品销售表现查看", "product", "商品管理", false),
            permission(PRODUCT_SALES_THRESHOLD_VIEW, "商品销售阈值查看", "product", "商品管理", false),
            permission(PRODUCT_SALES_THRESHOLD_CONFIG, "商品销售阈值配置", "product", "商品管理", true),
            permission(PRODUCT_VIOLATION_HANDLE, "商品违规处理", "product", "商品管理", true),
            permission(CATEGORY_MANAGE, "类目管理（兼容旧权限）", "product", "商品管理", false),
            permission(CATEGORY_VIEW, "类目查看", "product", "商品管理", false),
            permission(CATEGORY_CREATE, "类目创建", "product", "商品管理", false),
            permission(CATEGORY_UPDATE, "类目更新", "product", "商品管理", false),
            permission(CATEGORY_STATUS_UPDATE, "类目状态调整", "product", "商品管理", false),
            permission(CATEGORY_DELETE, "类目删除", "product", "商品管理", true),

            permission(STOCK_VIEW, "库存查看", "stock", "库存管理", false),
            permission(STOCK_LOG_VIEW, "库存流水查看", "stock", "库存管理", false),
            permission(STOCK_ADJUST, "库存调整", "stock", "库存管理", true),
            permission(STOCK_POLICY_UPDATE, "库存策略调整", "stock", "库存管理", true),
            permission(STOCK_WARNING_VIEW, "库存预警查看", "stock", "库存管理", false),
            permission(STOCK_WARNING_HANDLE, "库存预警处理", "stock", "库存管理", false),
            permission(STOCK_RECONCILE_VIEW, "库存对账查看", "stock", "库存管理", false),
            permission(STOCK_RECONCILE_CHECK, "库存一致性校验", "stock", "库存管理", true),
            permission(STOCK_RECONCILE_REPAIR, "库存对账修复", "stock", "库存管理", true),

            permission(ORDER_VIEW, "订单列表查看", "order", "订单管理", false),
            permission(ORDER_DETAIL_VIEW, "订单详情查看", "order", "订单管理", false),
            permission(ORDER_REMARK, "订单备注", "order", "订单管理", false),
            permission(ORDER_RECEIVER_UPDATE, "订单收货信息修改", "order", "订单管理", false),
            permission(ORDER_SHIP, "订单发货/完结", "order", "订单管理", true),
            permission(ORDER_CLOSE, "订单关闭", "order", "订单管理", true),
            permission(ORDER_EXCEPTION_VIEW, "订单异常查看", "order", "订单管理", false),
            permission(ORDER_EXCEPTION_HANDLE, "订单异常处理", "order", "订单管理", true),
            permission(ORDER_PAYMENT_EXCEPTION_HANDLE, "支付异常处理", "order", "订单管理", true),
            permission(ORDER_CONFIRM_PAID, "人工确认支付", "order", "订单管理", true),
            permission(ORDER_SKU_SWITCH, "订单 SKU 切换", "order", "订单管理", true),
            permission(ORDER_LOG_VIEW, "订单日志查看", "order", "订单管理", false),

            permission(AFTERSALE_VIEW, "售后列表查看", "aftersale", "售后退款", false),
            permission(AFTERSALE_DETAIL_VIEW, "售后详情查看", "aftersale", "售后退款", false),
            permission(AFTERSALE_AUDIT, "售后审核", "aftersale", "售后退款", true),
            permission(AFTERSALE_REVIEW, "售后复核", "aftersale", "售后退款", true),
            permission(AFTERSALE_REFUND_VIEW, "售后退款查看", "aftersale", "售后退款", false),

            permission(REFUND_VIEW, "退款查看", "finance", "支付财务", false),
            permission(REFUND_EXECUTE, "退款/支付关闭执行", "finance", "支付财务", true),
            permission(REFUND_SYNC, "退款状态同步", "finance", "支付财务", true),
            permission(FINANCE_VIEW, "财务业务查看", "finance", "支付财务", false),
            permission(PAYMENT_VIEW, "支付单查看", "finance", "支付财务", false),
            permission(PAYMENT_DETAIL_VIEW, "支付单详情查看", "finance", "支付财务", false),
            permission(PAYMENT_CLOSE, "支付单关闭", "finance", "支付财务", true),
            permission(PAYMENT_SYNC, "支付状态同步", "finance", "支付财务", true),
            permission(PAYMENT_REPAIR, "支付状态修复", "finance", "支付财务", true),
            permission(PAYMENT_CALLBACK_VIEW, "支付回调查看", "finance", "支付财务", false),

            permission(RECONCILIATION_VIEW, "对账查看", "reconciliation", "对账闭环", false),
            permission(RECONCILIATION_TASK_CREATE, "对账任务创建", "reconciliation", "对账闭环", true),
            permission(RECONCILIATION_TASK_RUN, "对账任务执行", "reconciliation", "对账闭环", true),
            permission(RECONCILIATION_TASK_ARCHIVE, "对账任务归档", "reconciliation", "对账闭环", true),
            permission(RECONCILIATION_BILL_IMPORT, "渠道账单导入", "reconciliation", "对账闭环", true),
            permission(RECONCILIATION_DIFF_HANDLE, "对账差异处理", "reconciliation", "对账闭环", true),
            permission(RECONCILIATION_DIFF_REPAIR, "对账差异修复", "reconciliation", "对账闭环", true),
            permission(RECONCILIATION_HANGING_FOLLOW, "挂账跟进闭环", "reconciliation", "对账闭环", true),
            permission(RECONCILIATION_HANDLE, "对账处理（兼容旧权限）", "reconciliation", "对账闭环", true)
    );

    private static final List<String> ALL_PERMISSIONS = PERMISSION_DEFINITIONS.stream().map(AdminPermissionDefinition::code).toList();

    private static final List<String> FINANCE_OPERATOR = List.of(
            DASHBOARD_VIEW, DASHBOARD_FINANCE_VIEW,
            FINANCE_VIEW, PAYMENT_VIEW, PAYMENT_DETAIL_VIEW, PAYMENT_SYNC, PAYMENT_REPAIR, PAYMENT_CLOSE, PAYMENT_CALLBACK_VIEW,
            REFUND_VIEW, REFUND_SYNC, REFUND_EXECUTE,
            RECONCILIATION_VIEW, RECONCILIATION_TASK_CREATE, RECONCILIATION_TASK_RUN, RECONCILIATION_TASK_ARCHIVE, RECONCILIATION_BILL_IMPORT, RECONCILIATION_DIFF_HANDLE, RECONCILIATION_DIFF_REPAIR, RECONCILIATION_HANGING_FOLLOW, RECONCILIATION_HANDLE,
            ORDER_VIEW, ORDER_DETAIL_VIEW, ORDER_EXCEPTION_VIEW, ORDER_LOG_VIEW,
            AFTERSALE_VIEW, AFTERSALE_DETAIL_VIEW, AFTERSALE_REFUND_VIEW
    );

    private static final List<String> ORDER_OPERATOR = List.of(
            DASHBOARD_VIEW, DASHBOARD_OPERATIONS_VIEW,
            USER_VIEW, USER_DETAIL_VIEW, USER_ADDRESS_VIEW,
            ORDER_VIEW, ORDER_DETAIL_VIEW, ORDER_REMARK, ORDER_RECEIVER_UPDATE, ORDER_SHIP, ORDER_CLOSE, ORDER_EXCEPTION_VIEW, ORDER_EXCEPTION_HANDLE, ORDER_PAYMENT_EXCEPTION_HANDLE, ORDER_CONFIRM_PAID, ORDER_SKU_SWITCH, ORDER_LOG_VIEW,
            AFTERSALE_VIEW, AFTERSALE_DETAIL_VIEW, AFTERSALE_AUDIT, AFTERSALE_REVIEW, AFTERSALE_REFUND_VIEW,
            PRODUCT_VIEW, PRODUCT_DETAIL_VIEW,
            PAYMENT_VIEW, PAYMENT_DETAIL_VIEW, REFUND_VIEW
    );

    private static final List<String> WAREHOUSE_OPERATOR = List.of(
            DASHBOARD_VIEW, DASHBOARD_WAREHOUSE_VIEW,
            STOCK_VIEW, STOCK_LOG_VIEW, STOCK_ADJUST, STOCK_POLICY_UPDATE, STOCK_WARNING_VIEW, STOCK_WARNING_HANDLE, STOCK_RECONCILE_VIEW, STOCK_RECONCILE_CHECK, STOCK_RECONCILE_REPAIR,
            ORDER_VIEW, ORDER_DETAIL_VIEW, ORDER_SHIP, ORDER_LOG_VIEW,
            PRODUCT_VIEW, PRODUCT_DETAIL_VIEW
    );

    private static final List<String> PRODUCT_OPERATOR = List.of(
            DASHBOARD_VIEW, DASHBOARD_PRODUCTS_VIEW,
            PRODUCT_VIEW, PRODUCT_DETAIL_VIEW, PRODUCT_CREATE, PRODUCT_UPDATE, PRODUCT_ON_SALE, PRODUCT_OFF_SALE, PRODUCT_STATUS_UPDATE, PRODUCT_SALES_VIEW, PRODUCT_SALES_THRESHOLD_VIEW, PRODUCT_SALES_THRESHOLD_CONFIG, PRODUCT_VIOLATION_HANDLE, CATEGORY_MANAGE, CATEGORY_VIEW, CATEGORY_CREATE, CATEGORY_UPDATE, CATEGORY_STATUS_UPDATE, CATEGORY_DELETE,
            STOCK_VIEW, STOCK_WARNING_VIEW,
            ORDER_VIEW, ORDER_DETAIL_VIEW
    );

    private static final Map<String, List<String>> ROLE_DEFAULT_PERMISSIONS = new LinkedHashMap<>();
    private static final Map<String, List<String>> ROLE_PERMISSION_SCOPES = new LinkedHashMap<>();
    private static final List<AdminRole> BUILT_IN_ROLES;

    static {
        ROLE_DEFAULT_PERMISSIONS.put("SUPER_ADMIN", ALL_PERMISSIONS);
        ROLE_DEFAULT_PERMISSIONS.put("FINANCE_OPERATOR", FINANCE_OPERATOR);
        ROLE_DEFAULT_PERMISSIONS.put("ORDER_OPERATOR", ORDER_OPERATOR);
        ROLE_DEFAULT_PERMISSIONS.put("WAREHOUSE_OPERATOR", WAREHOUSE_OPERATOR);
        ROLE_DEFAULT_PERMISSIONS.put("PRODUCT_OPERATOR", PRODUCT_OPERATOR);

        ROLE_PERMISSION_SCOPES.put("SUPER_ADMIN", ALL_PERMISSIONS);
        ROLE_PERMISSION_SCOPES.put("FINANCE_OPERATOR", append(FINANCE_OPERATOR, LOG_OPERATION_VIEW));
        ROLE_PERMISSION_SCOPES.put("ORDER_OPERATOR", append(ORDER_OPERATOR, USER_EDIT, USER_DISABLE));
        ROLE_PERMISSION_SCOPES.put("WAREHOUSE_OPERATOR", append(WAREHOUSE_OPERATOR, ORDER_CLOSE, LOG_OPERATION_VIEW));
        ROLE_PERMISSION_SCOPES.put("PRODUCT_OPERATOR", append(PRODUCT_OPERATOR, STOCK_LOG_VIEW));

        BUILT_IN_ROLES = List.of(
                new AdminRole("SUPER_ADMIN", "超级管理员", ROLE_DEFAULT_PERMISSIONS.get("SUPER_ADMIN"), true),
                new AdminRole("FINANCE_OPERATOR", "财务人员", ROLE_DEFAULT_PERMISSIONS.get("FINANCE_OPERATOR"), true),
                new AdminRole("ORDER_OPERATOR", "订单运营", ROLE_DEFAULT_PERMISSIONS.get("ORDER_OPERATOR"), true),
                new AdminRole("WAREHOUSE_OPERATOR", "仓储人员", ROLE_DEFAULT_PERMISSIONS.get("WAREHOUSE_OPERATOR"), true),
                new AdminRole("PRODUCT_OPERATOR", "商品运营", ROLE_DEFAULT_PERMISSIONS.get("PRODUCT_OPERATOR"), true)
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
            case "FINANCE", "PAY", "FINANCE_STAFF", "FINANCE_MANAGER" -> "FINANCE_OPERATOR";
            case "ORDER", "ORDER_STAFF", "ORDER_MANAGER", "CUSTOMER", "CUSTOMER_SERVICE", "CUSTOMER_SERVICE_MANAGER", "CUSTOMER_SERVICE_OPERATOR", "SERVICE", "SERVICE_MANAGER", "USER_OPERATOR" -> "ORDER_OPERATOR";
            case "STOCK", "WAREHOUSE", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF", "STOCK_OPERATOR" -> "WAREHOUSE_OPERATOR";
            case "PRODUCT", "PRODUCT_MANAGER", "PRODUCT_STAFF", "OPERATION", "OPERATION_STAFF", "OPERATION_MANAGER", "OPERATION_OPERATOR", "OPERATION_LEADER", "ADMIN_MANAGER", "PLATFORM", "MANAGER" -> "PRODUCT_OPERATOR";
            case "AUDIT", "AUDIT_OPERATOR" -> "FINANCE_OPERATOR";
            default -> normalized;
        };
    }

    public static List<String> superAdminPermissions() {
        return defaultPermissions("SUPER_ADMIN");
    }

    public static List<String> allPermissions() {
        return ALL_PERMISSIONS;
    }

    public static List<AdminPermissionDefinition> permissionDefinitions() {
        return PERMISSION_DEFINITIONS;
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

    private static AdminPermissionDefinition permission(String code, String name, String groupCode, String groupName, boolean sensitive) {
        return new AdminPermissionDefinition(code, name, groupCode, groupName, sensitive);
    }

    private static List<String> append(List<String> base, String... permissions) {
        return Stream.concat(base.stream(), Stream.of(permissions)).distinct().toList();
    }
}
