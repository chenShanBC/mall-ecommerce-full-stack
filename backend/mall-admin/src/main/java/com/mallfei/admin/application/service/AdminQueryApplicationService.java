package com.mallfei.admin.application.service;

import com.mallfei.admin.application.assembler.AdminViewAssembler;
import com.mallfei.admin.application.vo.*;
import com.mallfei.aftersale.domain.model.AftersaleOrder;
import com.mallfei.aftersale.facade.AftersaleFacade;
import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.common.api.PageResult;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.facade.OrderFacade;
import com.mallfei.pay.application.vo.PayReconcileResultView;
import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.pay.domain.service.PayOrderDomainService;
import com.mallfei.pay.facade.PayFacade;
import com.mallfei.stock.application.dto.StockQuery;
import com.mallfei.stock.facade.StockFacade;
import com.mallfei.stock.facade.StockOperationLogSnapshot;
import com.mallfei.stock.facade.StockSnapshot;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminQueryApplicationService {

    private static final long DASHBOARD_CACHE_TTL_MILLIS = 30_000L;

    private final AuthFacade authFacade;
    private final OrderFacade orderFacade;
    private final PayFacade payFacade;
    private final PayOrderDomainService payOrderDomainService;
    private final AdminViewAssembler adminViewAssembler;
    private final StockFacade stockFacade;
    private final AftersaleFacade aftersaleFacade;

    private volatile AdminDashboardOverviewView dashboardCache;
    private volatile long dashboardCacheTime;

    public AdminQueryApplicationService(AuthFacade authFacade,
                                        OrderFacade orderFacade,
                                        PayFacade payFacade,
                                        PayOrderDomainService payOrderDomainService,
                                        AdminViewAssembler adminViewAssembler,
                                        StockFacade stockFacade,
                                        AftersaleFacade aftersaleFacade) {
        this.authFacade = authFacade;
        this.orderFacade = orderFacade;
        this.payFacade = payFacade;
        this.payOrderDomainService = payOrderDomainService;
        this.adminViewAssembler = adminViewAssembler;
        this.stockFacade = stockFacade;
        this.aftersaleFacade = aftersaleFacade;
    }

    public PageResult<AdminOrderSummaryView> adminOrders(String status, String keyword, long page, long size, String sortBy, String sortOrder) {
        requireAdmin();
        PageResult<Order> result = orderFacade.search(status, blank(keyword) ? null : keyword.trim(), page, size, sortBy, sortOrder);
        return new PageResult<>(result.page(), result.size(), result.total(), result.pages(), result.records().stream().map(adminViewAssembler::toOrderSummary).toList());
    }

    public AdminOrderDetailView adminOrderDetail(String orderNo) {
        requireAdmin();
        return adminViewAssembler.toOrderDetail(orderFacade.getByOrderNo(orderNo));
    }

    public PageResult<AdminPaySummaryView> adminPays(String status, String keyword, long page, long size, String sortBy, String sortOrder) {
        requireAdmin();
        PageResult<PayOrder> result = payFacade.search(status, blank(keyword) ? null : keyword.trim(), page, size, sortBy, sortOrder);
        return new PageResult<>(result.page(), result.size(), result.total(), result.pages(), result.records().stream().map(adminViewAssembler::toPaySummary).toList());
    }

    public AdminPayDetailView adminPayDetail(String orderNo) {
        requireAdmin();
        return adminViewAssembler.toPayDetail(loadPayOrder(orderNo));
    }

    public PageResult<AdminAftersaleSummaryView> adminAftersales(String status, String keyword, long page, long size) {
        requireAdmin();
        List<AdminAftersaleSummaryView> rows = aftersaleFacade.findAll().stream()
                .filter(item -> blank(status) || status.equalsIgnoreCase(item.status()))
                .filter(item -> blank(keyword) || item.aftersaleNo().contains(keyword.trim()) || item.orderNo().contains(keyword.trim()))
                .map(item -> new AdminAftersaleSummaryView(item.id(), item.aftersaleNo(), item.orderNo(), item.userId(), item.aftersaleType(), item.status(), item.refundAmountCent(), item.reason(), item.createdAt()))
                .toList();
        return PageResult.of(rows, page, size);
    }

    public AdminAftersaleDetailView adminAftersaleDetail(String aftersaleNo) {
        requireAdmin();
        AftersaleOrder item = aftersaleFacade.getByAftersaleNo(aftersaleNo);
        return new AdminAftersaleDetailView(item.id(), item.aftersaleNo(), item.orderNo(), item.userId(), item.aftersaleType(), item.status(), item.refundAmountCent(), item.reason(), item.createdAt(), item.updatedAt());
    }

    public PageResult<AdminReconcileRowView> reconcileList(String status, long page, long size, String sortBy, String sortOrder) {
        requireAdmin();
        List<Order> orders = orderFacade.findAll();
        Map<String, PayOrder> payByOrderNo = payFacade.findAll().stream()
                .collect(Collectors.toMap(PayOrder::orderNo, Function.identity(), (left, right) -> left));
        List<AdminReconcileRowView> rows = orders.stream()
                .map(order -> reconcileRow(order, payByOrderNo.get(order.orderNo())))
                .filter(row -> blank(status) || status.equals(row.reconcileStatus()))
                .toList();
        return PageResult.of(sortList(rows, reconcileComparator(sortBy), blank(sortOrder) ? "asc" : sortOrder), page, size);
    }

    public AdminReconcileRowView reconcileRow(String orderNo) {
        requireAdmin();
        Order order = orderFacade.getByOrderNo(orderNo);
        return reconcileRow(order, payFacade.findByOrderNo(orderNo).orElse(null));
    }

    public AdminReconcileRowView reconcileRowView(String orderNo, PayReconcileResultView result) {
        requireAdmin();
        return new AdminReconcileRowView(
                null,
                result.orderNo(),
                result.orderStatus(),
                result.orderPayAmount(),
                result.payExists(),
                result.payOrderNo(),
                result.payStatus(),
                result.payAmount(),
                result.consistent(),
                result.consistent(),
                result.consistent() ? "CONSISTENT" : "ABNORMAL"
        );
    }

    public AdminDashboardOverviewView dashboardOverview() {
        requireAdmin();
        AdminDashboardOverviewView cached = dashboardCache;
        long now = System.currentTimeMillis();
        if (cached != null && now - dashboardCacheTime < DASHBOARD_CACHE_TTL_MILLIS) {
            return cached;
        }
        synchronized (this) {
            cached = dashboardCache;
            now = System.currentTimeMillis();
            if (cached != null && now - dashboardCacheTime < DASHBOARD_CACHE_TTL_MILLIS) {
                return cached;
            }
            AdminDashboardOverviewView overview = buildDashboardOverview();
            dashboardCache = overview;
            dashboardCacheTime = now;
            return overview;
        }
    }

    private AdminDashboardOverviewView buildDashboardOverview() {
        List<Order> orders = orderFacade.findAll();
        List<com.mallfei.pay.domain.model.PayOrder> pays = payFacade.findAll();
        Map<String, PayOrder> payByOrderNo = pays.stream()
                .collect(Collectors.toMap(PayOrder::orderNo, Function.identity(), (left, right) -> left));
        AdminDashboardStatsView stats = new AdminDashboardStatsView(
                orderFacade.countAll(),
                orderFacade.countByStatus(Order.STATUS_PENDING_PAYMENT),
                orderFacade.countByStatus(Order.STATUS_PAID),
                orderFacade.countByStatus(Order.STATUS_SHIPPED),
                orderFacade.countByStatus(Order.STATUS_COMPLETED),
                orderFacade.countCancelled(),
                orderFacade.sumPaidAmount(),
                payFacade.countAll(),
                payFacade.countPending(),
                payFacade.countSuccess(),
                payFacade.countClosed()
        );
        long lowCount = stockFacade.countByWarningStatus("LOW");
        long highCount = stockFacade.countByWarningStatus("HIGH");
        long normalCount = stockFacade.countByWarningStatus("NORMAL");
        AdminStockWarningStatsView warningStats = new AdminStockWarningStatsView(
                lowCount,
                highCount,
                normalCount,
                lowCount + highCount + normalCount
        );
        long abnormalReconcileCount = orders.stream()
                .filter(order -> {
                    PayOrder pay = payByOrderNo.get(order.orderNo());
                    return !payOrderDomainService.reconcileAmount(order, pay) || !payOrderDomainService.reconcileStatus(order, pay);
                })
                .count();
        List<AdminDashboardTodoView> todos = List.of(
                new AdminDashboardTodoView("order:pending-payment", "待支付订单", "需要关注支付转化", stats.pendingOrderCount(), stats.pendingOrderCount() > 0 ? "warning" : "info", "/orders", "status", "PENDING_PAYMENT"),
                new AdminDashboardTodoView("pay:pending", "待处理支付单", "建议尽快核查支付流转", stats.pendingPayCount(), stats.pendingPayCount() > 0 ? "warning" : "info", "/pays", "status", "PENDING"),
                new AdminDashboardTodoView("reconcile:abnormal", "对账异常", "建议优先处理异常对账", abnormalReconcileCount, abnormalReconcileCount > 0 ? "danger" : "info", "/reconciliations", "status", "ABNORMAL"),
                new AdminDashboardTodoView("stock:low-warning", "低库存预警", "存在补货压力 SKU", warningStats.lowCount(), warningStats.lowCount() > 0 ? "warning" : "info", "/stocks", "warningStatus", "LOW")
        );
        List<AdminDashboardShortcutView> shortcuts = List.of(
                new AdminDashboardShortcutView("待支付订单", "查看未完成支付的订单", "/orders", "status", "PENDING_PAYMENT", stats.pendingOrderCount()),
                new AdminDashboardShortcutView("待处理支付", "查看待处理支付单", "/pays", "status", "PENDING", stats.pendingPayCount()),
                new AdminDashboardShortcutView("异常对账", "进入异常对账列表", "/reconciliations", "status", "ABNORMAL", abnormalReconcileCount),
                new AdminDashboardShortcutView("低库存预警", "查看低库存预警 SKU", "/stocks", "warningStatus", "LOW", warningStats.lowCount())
        );
        return adminViewAssembler.toDashboardOverview(stats, warningStats, abnormalReconcileCount, todos, shortcuts);
    }

    public PageResult<StockSnapshot> stockList(Long skuId, String stockStatus, String warningStatus, long page, long size, String sortBy, String sortOrder) {
        requireAdmin();
        return stockFacade.stockList(new StockQuery(skuId, stockStatus, warningStatus, blank(sortBy) ? "skuId" : sortBy, blank(sortOrder) ? "asc" : sortOrder, page, size));
    }

    public PageResult<StockSnapshot> warningStocks(long page, long size) {
        requireAdmin();
        return stockFacade.stockList(new StockQuery(null, null, "LOW", "skuId", "asc", page, size));
    }

    public PageResult<AdminStockOperationLogView> stockLogs(Long skuId, String operationType, String startTime, String endTime, long page, long size, String sortBy, String sortOrder) {
        requireAdmin();
        LocalDateTime start = blank(startTime) ? null : LocalDateTime.parse(startTime);
        LocalDateTime end = blank(endTime) ? null : LocalDateTime.parse(endTime);
        PageResult<StockOperationLogSnapshot> result = stockFacade.pageLogs(skuId, operationType, start, end, 1, 1000);
        List<AdminStockOperationLogView> rows = result.records().stream().map(adminViewAssembler::toStockOperationLogView).toList();
        return PageResult.of(sortList(rows, stockLogComparator(sortBy), blank(sortOrder) ? "asc" : sortOrder), page, size);
    }

    private AdminReconcileRowView reconcileRow(Order order, PayOrder pay) {
        boolean amountOk = payOrderDomainService.reconcileAmount(order, pay);
        boolean statusOk = payOrderDomainService.reconcileStatus(order, pay);
        return adminViewAssembler.toReconcileRow(order, pay, amountOk, statusOk);
    }


    private Comparator<AdminReconcileRowView> reconcileComparator(String sortBy) {
        String actualSortBy = blank(sortBy) ? "orderId" : sortBy;
        return switch (actualSortBy) {
            case "orderId" -> Comparator.comparing(AdminReconcileRowView::orderId, Comparator.nullsLast(Long::compareTo));
            case "orderNo" -> Comparator.comparing(AdminReconcileRowView::orderNo, Comparator.nullsLast(String::compareTo));
            case "orderStatus" -> Comparator.comparing(AdminReconcileRowView::orderStatus, Comparator.nullsLast(String::compareTo));
            case "orderPayAmount" -> Comparator.comparing(AdminReconcileRowView::orderPayAmount, Comparator.nullsLast(Long::compareTo));
            case "payAmount" -> Comparator.comparing(AdminReconcileRowView::payAmount, Comparator.nullsLast(Long::compareTo));
            case "reconcileStatus" -> Comparator.comparing(AdminReconcileRowView::reconcileStatus, Comparator.nullsLast(String::compareTo));
            default -> Comparator.comparing(AdminReconcileRowView::orderId, Comparator.nullsLast(Long::compareTo));
        };
    }

    private Comparator<StockSnapshot> stockComparator(String sortBy) {
        String actualSortBy = blank(sortBy) ? "skuId" : sortBy;
        return switch (actualSortBy) {
            case "skuId" -> Comparator.comparing(StockSnapshot::skuId, Comparator.nullsLast(Long::compareTo));
            case "availableStock" -> Comparator.comparing(StockSnapshot::availableStock, Comparator.nullsLast(Integer::compareTo));
            case "lockedStock" -> Comparator.comparing(StockSnapshot::lockedStock, Comparator.nullsLast(Integer::compareTo));
            case "totalStock" -> Comparator.comparing(StockSnapshot::totalStock, Comparator.nullsLast(Integer::compareTo));
            case "stockStatus" -> Comparator.comparing(StockSnapshot::stockStatus, Comparator.nullsLast(String::compareTo));
            case "warningStatus" -> Comparator.comparing(StockSnapshot::warningStatus, Comparator.nullsLast(String::compareTo));
            case "lowStockThreshold" -> Comparator.comparing(StockSnapshot::lowStockThreshold, Comparator.nullsLast(Integer::compareTo));
            case "highStockThreshold" -> Comparator.comparing(StockSnapshot::highStockThreshold, Comparator.nullsLast(Integer::compareTo));
            default -> Comparator.comparing(StockSnapshot::skuId, Comparator.nullsLast(Long::compareTo));
        };
    }

    private Comparator<AdminStockOperationLogView> stockLogComparator(String sortBy) {
        String actualSortBy = blank(sortBy) ? "id" : sortBy;
        return switch (actualSortBy) {
            case "id" -> Comparator.comparing(AdminStockOperationLogView::id, Comparator.nullsLast(Long::compareTo));
            case "skuId" -> Comparator.comparing(AdminStockOperationLogView::skuId, Comparator.nullsLast(Long::compareTo));
            case "operationType" -> Comparator.comparing(AdminStockOperationLogView::operationType, Comparator.nullsLast(String::compareTo));
            case "businessType" -> Comparator.comparing(AdminStockOperationLogView::businessType, Comparator.nullsLast(String::compareTo));
            case "operatorName" -> Comparator.comparing(AdminStockOperationLogView::operatorName, Comparator.nullsLast(String::compareTo));
            case "changeQuantity" -> Comparator.comparing(AdminStockOperationLogView::changeQuantity, Comparator.nullsLast(Integer::compareTo));
            case "beforeAvailableStock" -> Comparator.comparing(AdminStockOperationLogView::beforeAvailableStock, Comparator.nullsLast(Integer::compareTo));
            case "afterAvailableStock" -> Comparator.comparing(AdminStockOperationLogView::afterAvailableStock, Comparator.nullsLast(Integer::compareTo));
            case "createdAt" -> Comparator.comparing(AdminStockOperationLogView::createdAt, Comparator.nullsLast(String::compareTo));
            default -> Comparator.comparing(AdminStockOperationLogView::id, Comparator.nullsLast(Long::compareTo));
        };
    }

    private <T> List<T> sortList(List<T> rows, Comparator<T> comparator, String sortOrder) {
        if (comparator == null) {
            return rows;
        }
        Comparator<T> actual = "desc".equalsIgnoreCase(sortOrder) ? comparator.reversed() : comparator;
        return rows.stream().sorted(actual).toList();
    }

    private PayOrder loadPayOrder(String orderNo) {
        return payFacade.findByOrderNo(orderNo).orElseThrow(() -> BusinessException.badRequest("支付单不存在"));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private void requireAdmin() {
        AuthenticatedPrincipal principal = authFacade.currentPrincipal();
        if (principal == null || !principal.isAdmin()) {
            throw BusinessException.forbidden("仅管理员可访问当前接口");
        }
    }
}
