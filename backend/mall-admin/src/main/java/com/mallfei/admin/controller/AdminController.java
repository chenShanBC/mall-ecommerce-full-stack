package com.mallfei.admin.controller;

import com.mallfei.admin.application.dto.*;
import com.mallfei.admin.application.service.*;
import com.mallfei.admin.application.vo.*;
import com.mallfei.common.api.*;
import com.mallfei.common.auth.*;
import com.mallfei.pay.application.vo.PayOrderView;
import com.mallfei.product.application.dto.*;
import com.mallfei.product.application.vo.*;
import com.mallfei.product.domain.model.Category;
import com.mallfei.product.facade.ProductFacade;
import com.mallfei.stock.facade.StockSnapshot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Validated
@Tag(name = "Admin Backend")
public class AdminController {
    private final AdminApplicationService app;
    private final AdminQueryApplicationService query;
    private final AdminCommandApplicationService command;
    private final AdminAccountManagementApplicationService account;
    private final AdminStockManagementApplicationService stock;
    private final AdminPayManagementApplicationService payManagement;
    private final AdminAftersaleManagementApplicationService aftersaleManagement;
    private final AdminProductManagementApplicationService productManagement;
    private final AdminUserManagementApplicationService userManagement;
    private final ProductFacade product;

    public AdminController(AdminApplicationService app,
                           AdminQueryApplicationService query,
                           AdminCommandApplicationService command,
                           AdminAccountManagementApplicationService account,
                           AdminStockManagementApplicationService stock,
                           AdminPayManagementApplicationService payManagement,
                           AdminAftersaleManagementApplicationService aftersaleManagement,
                           AdminProductManagementApplicationService productManagement,
                           AdminUserManagementApplicationService userManagement,
                           ProductFacade product) {
        this.app = app;
        this.query = query;
        this.command = command;
        this.account = account;
        this.stock = stock;
        this.payManagement = payManagement;
        this.aftersaleManagement = aftersaleManagement;
        this.productManagement = productManagement;
        this.userManagement = userManagement;
        this.product = product;
    }

    @Operation(summary = "管理员密码登录")
    @PostMapping("/login/password")
    public ApiResponse<AdminLoginResult> login(@Valid @RequestBody AdminPasswordLoginRequest req) { return ApiResponse.success(app.login(req)); }
    @RequireAdmin @Operation(summary = "管理员退出登录") @DeleteMapping("/logout") public ApiResponse<Boolean> logout() { app.logout(); return ApiResponse.success("退出成功", Boolean.TRUE); }
    @RequireAdmin @Operation(summary = "获取当前管理员信息") @GetMapping("/me") public ApiResponse<AuthenticatedPrincipal> me() { return ApiResponse.success(app.currentAdmin()); }
    @RequireAdmin @Operation(summary = "修改个人资料") @PutMapping("/me/profile") public ApiResponse<AuthenticatedPrincipal> updateMyProfile(@Valid @RequestBody AdminUpdateProfileRequest req) { return ApiResponse.success(app.updateProfile(req)); }
    @RequireAdmin @Operation(summary = "修改个人密码") @PutMapping("/me/password") public ApiResponse<Boolean> changeMyPassword(@Valid @RequestBody AdminChangePasswordRequest req) { return ApiResponse.success(app.changePassword(req)); }
    @RequireAdmin @Operation(summary = "获取后台看板数据") @GetMapping("/dashboard") public ApiResponse<AdminDashboardOverviewView> dashboard() { return ApiResponse.success(query.dashboardOverview()); }

    @RequireAdmin @RequirePermission("admin:view") @Operation(summary = "查询运营账号列表") @GetMapping("/accounts") public ApiResponse<PageResult<AdminAccountView>> accounts(@RequestParam(required = false) String keyword, @RequestParam(required = false) String roleCode, @RequestParam(required = false) String status, @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long size) { return ApiResponse.success(account.listAccounts(keyword, roleCode, status, page, size)); }
    @RequireAdmin @RequirePermission("role:view") @Operation(summary = "查询内置角色列表") @GetMapping("/roles") public ApiResponse<java.util.List<AdminRoleView>> roles() { return ApiResponse.success(account.roles()); }
    @RequireAdmin @RequirePermission("permission:view") @Operation(summary = "查询角色权限模板") @GetMapping("/accounts/permission-templates") public ApiResponse<java.util.Map<String, java.util.List<String>>> permissionTemplates() { return ApiResponse.success(account.permissionTemplates()); }
    @RequireAdmin @RequirePermission("admin:create") @Operation(summary = "创建运营账号") @PostMapping("/accounts") public ApiResponse<AdminAccountView> createAccount(@Valid @RequestBody AdminCreateAccountRequest req) { return ApiResponse.success(account.createAccount(req)); }
    @RequireAdmin @RequirePermission("admin:update") @Operation(summary = "分配运营角色") @PatchMapping("/accounts/{adminId}/role") public ApiResponse<AdminAccountView> assignRole(@PathVariable Long adminId, @Valid @RequestBody AdminAssignRoleRequest req) { return ApiResponse.success(account.assignRole(adminId, req)); }
    @RequireAdmin @RequirePermission("admin:disable") @Operation(summary = "禁用运营账号") @PatchMapping("/accounts/{adminId}/disable") public ApiResponse<AdminAccountView> disableAccount(@PathVariable Long adminId) { return ApiResponse.success(account.disableAccount(adminId)); }
    @RequireAdmin @RequirePermission("admin:disable") @Operation(summary = "启用运营账号") @PatchMapping("/accounts/{adminId}/enable") public ApiResponse<AdminAccountView> enableAccount(@PathVariable Long adminId) { return ApiResponse.success(account.enableAccount(adminId)); }
    @RequireAdmin @RequirePermission("permission:assign") @Operation(summary = "更新运营账号权限") @PutMapping("/accounts/{adminId}/permissions") public ApiResponse<AdminAccountView> updatePermissions(@PathVariable Long adminId, @Valid @RequestBody AdminUpdateAccountPermissionRequest req) { return ApiResponse.success(account.updatePermissions(adminId, req)); }
    @RequireAdmin @RequirePermission("user:view") @Operation(summary = "查询C端用户列表") @GetMapping("/users") public ApiResponse<PageResult<AdminUserListItemView>> users(@RequestParam(required = false) String keyword, @RequestParam(required = false) String status, @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortOrder, @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long size) { return ApiResponse.success(userManagement.users(keyword, status, sortBy, sortOrder, page, size)); }
    @RequireAdmin @RequirePermission("user:view") @Operation(summary = "查询C端用户详情") @GetMapping("/users/{userId}") public ApiResponse<AdminUserDetailView> userDetail(@PathVariable Long userId) { return ApiResponse.success(userManagement.userDetail(userId)); }
    @RequireAdmin @RequirePermission("user:disable") @Operation(summary = "禁用C端用户") @PatchMapping("/users/{userId}/disable") public ApiResponse<AdminUserDetailView> disableUser(@PathVariable Long userId) { return ApiResponse.success(userManagement.disableUser(userId)); }
    @RequireAdmin @RequirePermission("user:disable") @Operation(summary = "启用C端用户") @PatchMapping("/users/{userId}/enable") public ApiResponse<AdminUserDetailView> enableUser(@PathVariable Long userId) { return ApiResponse.success(userManagement.enableUser(userId)); }
    @RequireAdmin @RequirePermission("log:operation:view") @Operation(summary = "查询运营操作日志") @GetMapping("/operation-logs") public ApiResponse<PageResult<AdminOperationLogView>> operationLogs(@RequestParam(required = false) String keyword, @RequestParam(required = false) String module, @RequestParam(required = false) String result, @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortOrder, @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long size) { return ApiResponse.success(account.operationLogs(keyword, module, result, page, size, sortBy, sortOrder)); }

    @RequireAdmin @RequirePermission("order:view") @Operation(summary = "查询后台订单列表") @GetMapping("/orders") public ApiResponse<PageResult<AdminOrderSummaryView>> orders(@RequestParam(required = false) String status, @RequestParam(required = false) String keyword, @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortOrder, @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "10") long size) { return ApiResponse.success(query.adminOrders(status, keyword, page, size, sortBy, sortOrder)); }
    @RequireAdmin @RequirePermission("order:view") @Operation(summary = "获取后台订单详情") @GetMapping("/orders/{orderNo}") public ApiResponse<AdminOrderDetailView> orderDetail(@PathVariable String orderNo) { return ApiResponse.success(query.adminOrderDetail(orderNo)); }
    @RequireAdmin @RequirePermission("order:close") @Operation(summary = "后台取消订单") @DeleteMapping("/orders/{orderNo}") public ApiResponse<AdminOrderDetailView> cancelOrder(@PathVariable String orderNo) { return ApiResponse.success(command.cancelOrder(orderNo)); }
    @RequireAdmin @RequirePermission("order:ship") @Operation(summary = "后台发货") @PatchMapping("/orders/{orderNo}/ship") public ApiResponse<AdminOrderDetailView> shipOrder(@PathVariable String orderNo) { return ApiResponse.success(command.shipOrder(orderNo)); }
    @RequireAdmin @RequirePermission("order:ship") @Operation(summary = "后台完结订单") @PatchMapping("/orders/{orderNo}/complete") public ApiResponse<AdminOrderDetailView> completeOrder(@PathVariable String orderNo) { return ApiResponse.success(command.completeOrder(orderNo)); }
    @RequireAdmin @RequirePermission("order:remark") @Operation(summary = "处理订单异常") @PostMapping("/orders/{orderNo}/exception-handle") public ApiResponse<AdminOrderOperationResultView> handleOrderException(@PathVariable String orderNo, @Valid @RequestBody AdminOrderExceptionHandleRequest req) { return ApiResponse.success(command.handleException(orderNo, req)); }

    @RequireAdmin @RequirePermission("payment:view") @Operation(summary = "查询后台支付单列表") @GetMapping("/pays") public ApiResponse<PageResult<AdminPaySummaryView>> pays(@RequestParam(required = false) String status, @RequestParam(required = false) String keyword, @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortOrder, @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "10") long size) { return ApiResponse.success(query.adminPays(status, keyword, page, size, sortBy, sortOrder)); }
    @RequireAdmin @RequirePermission("payment:view") @Operation(summary = "获取后台支付单详情") @GetMapping("/pays/{orderNo}") public ApiResponse<AdminPayDetailView> payDetail(@PathVariable String orderNo) { return ApiResponse.success(query.adminPayDetail(orderNo)); }
    @RequireAdmin @RequirePermission("refund:execute") @Operation(summary = "后台关闭支付单") @PostMapping("/pays/{orderNo}/close") public ApiResponse<PayOrderView> closePayOrder(@PathVariable String orderNo, @Valid @RequestBody AdminClosePayOrderRequest req) { return ApiResponse.success(payManagement.closePayOrder(orderNo, req)); }
    @RequireAdmin @RequirePermission("payment:view") @Operation(summary = "后台同步订单支付状态") @PostMapping("/pays/{orderNo}/sync-status") public ApiResponse<PayOrderView> syncPayOrderStatus(@PathVariable String orderNo) { return ApiResponse.success(payManagement.syncOrderStatus(orderNo)); }
    @RequireAdmin @RequirePermission("payment:view") @Operation(summary = "后台补偿已成功支付订单状态") @PostMapping("/pays/{orderNo}/repair-paid") public ApiResponse<PayOrderView> repairPaidOrder(@PathVariable String orderNo) { return ApiResponse.success(payManagement.repairPaidOrder(orderNo)); }

    @RequireAdmin @RequirePermission("aftersale:view") @Operation(summary = "查询后台售后单列表") @GetMapping("/aftersales") public ApiResponse<PageResult<AdminAftersaleSummaryView>> aftersales(@RequestParam(required = false) String status, @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "10") long size) { return ApiResponse.success(query.adminAftersales(status, keyword, page, size)); }
    @RequireAdmin @RequirePermission("aftersale:view") @Operation(summary = "获取后台售后单详情") @GetMapping("/aftersales/{aftersaleNo}") public ApiResponse<AdminAftersaleDetailView> aftersaleDetail(@PathVariable String aftersaleNo) { return ApiResponse.success(query.adminAftersaleDetail(aftersaleNo)); }
    @RequireAdmin @RequirePermission("aftersale:audit") @Operation(summary = "后台审核售后单") @PostMapping("/aftersales/{aftersaleNo}/review") public ApiResponse<AdminAftersaleDetailView> reviewAftersale(@PathVariable String aftersaleNo, @Valid @RequestBody AdminAftersaleReviewRequest req) { return ApiResponse.success(aftersaleManagement.review(aftersaleNo, req)); }

    @RequireAdmin @RequirePermission("reconciliation:view") @Operation(summary = "查询后台对账列表") @GetMapping("/reconciliations") public ApiResponse<PageResult<AdminReconcileRowView>> reconciliations(@RequestParam(required = false) String status, @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortOrder, @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "10") long size) { return ApiResponse.success(query.reconcileList(status, page, size, sortBy, sortOrder)); }
    @RequireAdmin @RequirePermission("reconciliation:handle") @Operation(summary = "执行单笔对账") @PostMapping("/reconciliations/{orderNo}/run") public ApiResponse<AdminReconcileRowView> runReconcile(@PathVariable String orderNo) { return ApiResponse.success(payManagement.reconcile(orderNo)); }
    @RequireAdmin @RequirePermission("reconciliation:handle") @Operation(summary = "处理对账异常") @PostMapping("/reconciliations/{orderNo}/handle") public ApiResponse<AdminReconcileRowView> handleReconcile(@PathVariable String orderNo, @Valid @RequestBody AdminReconcileHandleRequest req) { return ApiResponse.success(payManagement.handleReconcile(orderNo, req)); }

    @RequireAdmin @RequirePermission("stock:view") @Operation(summary = "查询后台库存列表") @GetMapping("/stocks") public ApiResponse<PageResult<StockSnapshot>> stocks(@RequestParam(required = false) Long skuId, @RequestParam(required = false) String stockStatus, @RequestParam(required = false) String warningStatus, @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortOrder, @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long size) { return ApiResponse.success(query.stockList(skuId, stockStatus, warningStatus, page, size, sortBy, sortOrder)); }
    @RequireAdmin @RequirePermission("stock:view") @Operation(summary = "查询库存预警列表") @GetMapping("/stocks/warnings") public ApiResponse<PageResult<StockSnapshot>> warningStocks(@RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long size) { return ApiResponse.success(query.warningStocks(page, size)); }
    @RequireAdmin @RequirePermission("stock:view") @Operation(summary = "查询库存操作日志") @GetMapping("/stocks/logs") public ApiResponse<PageResult<AdminStockOperationLogView>> stockLogs(@RequestParam(required = false) Long skuId, @RequestParam(required = false) String operationType, @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime, @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortOrder, @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long size) { return ApiResponse.success(query.stockLogs(skuId, operationType, startTime, endTime, page, size, sortBy, sortOrder)); }
    @RequireAdmin @RequirePermission("stock:adjust") @Operation(summary = "后台调整库存策略") @PutMapping("/stocks/{skuId}/policy") public ApiResponse<StockSnapshot> updateStockPolicy(@PathVariable Long skuId, @Valid @RequestBody AdminStockPolicyUpdateRequest req) { return ApiResponse.success(stock.updatePolicy(skuId, req)); }
    @RequireAdmin @RequirePermission("stock:adjust") @Operation(summary = "后台手工调整库存") @PutMapping("/stocks/{skuId}/adjust") public ApiResponse<StockSnapshot> adjustStock(@PathVariable Long skuId, @Valid @RequestBody AdminStockAdjustRequest req) { return ApiResponse.success(stock.adjust(skuId, req)); }
    @RequireAdmin @RequirePermission("stock:adjust") @Operation(summary = "后台处理库存预警") @PostMapping("/stocks/{skuId}/warning-handle") public ApiResponse<StockSnapshot> handleStockWarning(@PathVariable Long skuId, @Valid @RequestBody AdminStockWarningHandleRequest req) { return ApiResponse.success(stock.handleWarning(skuId, req)); }

    @RequireAdmin @RequirePermission("product:view") @Operation(summary = "获取后台类目列表") @GetMapping("/categories") public ApiResponse<java.util.List<CategoryAdminView>> categories() { return ApiResponse.success(product.adminCategories()); }
    @RequireAdmin @RequirePermission("category:manage") @Operation(summary = "新增类目") @PostMapping("/categories") public ApiResponse<Category> createCategory(@Valid @RequestBody AdminCreateCategoryRequest req) { return ApiResponse.success(product.createCategory(req.name(), req.parentId(), req.sortOrder())); }
    @RequireAdmin @RequirePermission("category:manage") @Operation(summary = "修改类目") @PutMapping("/categories/{categoryId}") public ApiResponse<Category> updateCategory(@PathVariable Long categoryId, @Valid @RequestBody AdminUpdateCategoryRequest req) { return ApiResponse.success(product.updateCategory(categoryId, req.name(), req.parentId(), req.sortOrder(), req.status())); }
    @RequireAdmin @RequirePermission("product:view") @Operation(summary = "查询后台商品列表") @GetMapping("/products") public ApiResponse<PageResponse<AdminProductPageRowView>> productPage(@RequestParam(required = false) String keyword, @RequestParam(required = false) Long categoryId, @RequestParam(required = false) String status, @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortOrder, @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "10") long size) { return ApiResponse.success(product.adminProductPage(new AdminProductPageQuery(keyword, categoryId, status, sortBy, sortOrder, page, size))); }
    @RequireAdmin @RequirePermission("product:view") @Operation(summary = "获取后台商品详情") @GetMapping("/products/{productId}") public ApiResponse<AdminProductDetailView> productDetail(@PathVariable Long productId) { return ApiResponse.success(product.adminProductDetail(productId)); }
    @RequireAdmin @RequirePermission("product:create") @Operation(summary = "新增商品") @PostMapping("/products") public ApiResponse<AdminProductSummaryView> createProduct(@Valid @RequestBody AdminCreateProductRequest req) { return ApiResponse.success(product.createProduct(req)); }
    @RequireAdmin @RequirePermission("product:update") @Operation(summary = "修改商品") @PutMapping("/products/{productId}") public ApiResponse<AdminProductSummaryView> updateProduct(@PathVariable Long productId, @Valid @RequestBody AdminUpdateProductRequest req) { return ApiResponse.success(product.updateProduct(productId, req)); }
    @RequireAdmin @RequirePermission("product:update") @Operation(summary = "修改商品状态") @PatchMapping("/products/{productId}/status") public ApiResponse<AdminProductSummaryView> updateProductStatus(@PathVariable Long productId, @Valid @RequestBody AdminProductStatusOperateRequest req) { return ApiResponse.success(productManagement.updateStatus(productId, req)); }
    @RequireAdmin @RequirePermission("product:update") @Operation(summary = "处理商品运营事件") @PostMapping("/products/{productId}/violation-handle") public ApiResponse<AdminProductSummaryView> handleProductViolation(@PathVariable Long productId, @Valid @RequestBody AdminProductViolationHandleRequest req) { return ApiResponse.success(productManagement.handleViolation(productId, req)); }

    public record AdminCreateCategoryRequest(@NotBlank(message = "类目名称不能为空") String name, Long parentId, Integer sortOrder) {}
    public record AdminUpdateCategoryRequest(@NotBlank(message = "类目名称不能为空") String name, Long parentId, Integer sortOrder, @NotBlank(message = "类目状态不能为空") String status) {}
}