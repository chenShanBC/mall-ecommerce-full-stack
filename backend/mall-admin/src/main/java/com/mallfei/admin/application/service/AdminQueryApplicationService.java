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
import com.mallfei.pay.domain.model.PayCallbackRecord;
import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.pay.domain.model.PayReconciliationRecord;
import com.mallfei.pay.domain.service.PayOrderDomainService;
import com.mallfei.pay.facade.PayFacade;
import com.mallfei.product.application.vo.AdminProductDetailView;
import com.mallfei.product.application.vo.AdminProductSkuEditView;
import com.mallfei.product.domain.model.ProductSpu;
import com.mallfei.product.facade.ProductFacade;
import com.mallfei.product.facade.ProductSkuSnapshot;
import com.mallfei.stock.application.dto.StockQuery;
import com.mallfei.stock.facade.StockFacade;
import com.mallfei.stock.facade.StockOperationLogSnapshot;
import com.mallfei.stock.facade.StockSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminQueryApplicationService {

    private final AuthFacade authFacade;
    private final OrderFacade orderFacade;
    private final PayFacade payFacade;
    private final PayOrderDomainService payOrderDomainService;
    private final AdminViewAssembler adminViewAssembler;
    private final StockFacade stockFacade;
    private final ProductFacade productFacade;
    private final AftersaleFacade aftersaleFacade;
    private final AdminOperationConfigApplicationService operationConfigApplicationService;
    private final JdbcTemplate jdbcTemplate;

    public AdminQueryApplicationService(AuthFacade authFacade,
                                        OrderFacade orderFacade,
                                        PayFacade payFacade,
                                        PayOrderDomainService payOrderDomainService,
                                        AdminViewAssembler adminViewAssembler,
                                        StockFacade stockFacade,
                                        ProductFacade productFacade,
                                        AftersaleFacade aftersaleFacade,
                                        AdminOperationConfigApplicationService operationConfigApplicationService,
                                        JdbcTemplate jdbcTemplate) {
        this.authFacade = authFacade;
        this.orderFacade = orderFacade;
        this.payFacade = payFacade;
        this.payOrderDomainService = payOrderDomainService;
        this.adminViewAssembler = adminViewAssembler;
        this.stockFacade = stockFacade;
        this.productFacade = productFacade;
        this.aftersaleFacade = aftersaleFacade;
        this.operationConfigApplicationService = operationConfigApplicationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<AdminOrderSummaryView> adminOrders(String status, String keyword, java.time.LocalDate startDate, java.time.LocalDate endDate, long page, long size, String sortBy, String sortOrder) {
        requireAdmin();
        PageResult<Order> result = orderFacade.search(status, blank(keyword) ? null : keyword.trim(), startDate, endDate, page, size, sortBy, sortOrder);
        return new PageResult<>(result.page(), result.size(), result.total(), result.pages(), result.records().stream()
                .map(adminViewAssembler::toOrderSummary)
                .toList());
    }

    public AdminOrderDetailView adminOrderDetail(String orderNo) {
        requireAdmin();
        return adminViewAssembler.toOrderDetail(orderFacade.getByOrderNo(orderNo));
    }

    public List<AdminOrderSkuSwitchOptionView> orderSkuSwitchOptions(String orderNo, Long orderItemId) {
        requireAdmin();
        if (orderItemId == null) {
            throw BusinessException.badRequest("订单商品项不能为空");
        }
        AdminOrderDetailView order = adminViewAssembler.toOrderDetail(orderFacade.getByOrderNo(orderNo));
        AdminOrderItemView orderItem = order.items().stream()
                .filter(item -> orderItemId.equals(item.id()))
                .findFirst()
                .orElseThrow(() -> BusinessException.badRequest("订单商品项不存在"));
        ProductSkuSnapshot currentSku = productFacade.getSkuSnapshot(orderItem.skuId());
        AdminProductDetailView product = productFacade.adminProductDetail(currentSku.spuId());
        return product.skus().stream()
                .filter(sku -> !sku.id().equals(orderItem.skuId()))
                .map(sku -> toSkuSwitchOption(sku, orderItem.salePrice()))
                .toList();
    }

    private AdminOrderSkuSwitchOptionView toSkuSwitchOption(AdminProductSkuEditView sku, Long currentSalePrice) {
        return new AdminOrderSkuSwitchOptionView(sku.id(), sku.skuName(), sku.skuCode(), sku.specJson(), sku.salePriceCent(), sku.availableStock(), sku.status(), currentSalePrice != null && currentSalePrice.equals(sku.salePriceCent()));
    }

    public PageResult<AdminPaySummaryView> adminPays(String status, String keyword, LocalDate startDate, LocalDate endDate, long page, long size, String sortBy, String sortOrder) {
        requireAdmin();
        PageResult<PayOrder> result = payFacade.search(status, blank(keyword) ? null : keyword.trim(), startDate, endDate, page, size, sortBy, sortOrder);
        return new PageResult<>(result.page(), result.size(), result.total(), result.pages(), result.records().stream().map(adminViewAssembler::toPaySummary).toList());
    }

    public AdminPayDetailView adminPayDetail(String orderNo) {
        requireAdmin();
        return adminViewAssembler.toPayDetail(loadPayOrder(orderNo));
    }

    public PageResult<AdminRefundView> adminRefunds(String orderNo, String status, long page, long size) {
        requireAdmin();
        List<AdminRefundView> rows = orderFacade.refundsByOrderNo(orderNo).stream()
                .filter(refund -> blank(status) || status.equalsIgnoreCase(refund.refundStatus()))
                .map(this::toAdminRefundView)
                .sorted(Comparator.comparing(AdminRefundView::id, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList();
        return PageResult.of(rows, page, size);
    }

    public PageResult<AdminRefundView> adminRefundsByKeyword(String status, String keyword, LocalDate startDate, LocalDate endDate, long page, long size) {
        requireAdmin();
        List<AdminRefundView> rows = orderFacade.searchRefunds(blank(status) ? null : status.trim(), blank(keyword) ? null : keyword.trim(), startDate, endDate).stream()
                .map(this::toAdminRefundView)
                .toList();
        return PageResult.of(rows, page, size);
    }

    private AdminRefundView toAdminRefundView(com.mallfei.order.domain.model.OrderRefund refund) {
        String payChannel = payFacade.findByOrderNo(refund.orderNo()).map(PayOrder::payChannel).orElse("");
        return new AdminRefundView(refund.id(), refund.refundNo(), refund.orderNo(), refund.userId(), refund.refundAmountCent(), payChannel, refund.channelRefundNo(), refund.refundStatus(), refund.refundReason(), refund.failReason(), refund.createdAt(), refund.updatedAt(),
                orderFacade.refundItemsByRefundNo(refund.refundNo()).stream()
                        .map(item -> new AdminRefundItemView(item.id(), item.orderItemId(), item.skuId(), item.quantity(), item.refundAmountCent()))
                        .toList());
    }

    public PageResult<AdminPayCallbackRecordView> payCallbackRecords(String processStatus, String keyword, long page, long size) {
        requireAdmin();
        PageResult<PayCallbackRecord> result = payFacade.searchCallbacks(processStatus, blank(keyword) ? null : keyword.trim(), page, size);
        return new PageResult<>(result.page(), result.size(), result.total(), result.pages(), result.records().stream()
                .map(record -> new AdminPayCallbackRecordView(record.id(), record.channel(), record.callbackType(), record.payOrderNo(), record.refundNo(), record.orderNo(), record.outTradeNo(), record.transactionNo(), record.amountCent(), record.tradeStatus(), record.verified(), record.processStatus(), record.failReason(), record.callbackTime(), record.processedAt(), record.createdAt()))
                .toList());
    }

    public AdminReconciliationOverviewView reconciliationOverview() {
        requireAdmin();
        List<Order> orders = orderFacade.findAll();
        Map<String, PayOrder> payByOrderNo = payFacade.findAll().stream()
                .collect(Collectors.toMap(PayOrder::orderNo, Function.identity(), (left, right) -> left));
        long paymentAbnormalCount = orders.stream()
                .filter(order -> {
                    PayOrder pay = payByOrderNo.get(order.orderNo());
                    return !payOrderDomainService.reconcileAmount(order, pay) || !payOrderDomainService.reconcileStatus(order, pay);
                })
                .count();
        long paymentTotalCount = orders.size();
        return new AdminReconciliationOverviewView(
                paymentTotalCount,
                Math.max(0, paymentTotalCount - paymentAbnormalCount),
                paymentAbnormalCount,
                payFacade.countReconciliationRecords(null, null, null),
                payFacade.countReconciliationRecords(null, Boolean.TRUE, null),
                payFacade.countReconciliationRecords(null, Boolean.FALSE, null),
                payFacade.countReconciliationRecords(null, Boolean.FALSE, PayReconciliationRecord.REPAIR_PENDING),
                payFacade.countReconciliationRecords(null, Boolean.FALSE, PayReconciliationRecord.REPAIR_DONE),
                payFacade.countReconciliationRecords(null, Boolean.FALSE, PayReconciliationRecord.REPAIR_IGNORED)
        );
    }

    public PageResult<AdminPayReconciliationRecordView> payReconciliationRecords(String bizType, String status, String repairStatus, String keyword, long page, long size) {
        requireAdmin();
        Boolean consistent = null;
        if ("CONSISTENT".equalsIgnoreCase(status)) {
            consistent = Boolean.TRUE;
        } else if ("ABNORMAL".equalsIgnoreCase(status)) {
            consistent = Boolean.FALSE;
        }
        PageResult<PayReconciliationRecord> result = payFacade.searchReconciliationRecords(bizType, consistent, repairStatus, blank(keyword) ? null : keyword.trim(), page, size);
        return new PageResult<>(result.page(), result.size(), result.total(), result.pages(), result.records().stream()
                .map(this::toPayReconciliationRecordView)
                .toList());
    }

    public AdminPayReconciliationRecordView payReconciliationRecord(Long id) {
        requireAdmin();
        return toPayReconciliationRecordView(payFacade.getReconciliationRecord(id));
    }

    public AdminPayReconciliationRecordView ignorePayReconciliationRecord(Long id, String remark) {
        requireAdmin();
        return toPayReconciliationRecordView(payFacade.markReconciliationRecordIgnored(id, remark));
    }

    public AdminPayReconciliationRecordView donePayReconciliationRecord(Long id, String remark) {
        requireAdmin();
        return toPayReconciliationRecordView(payFacade.markReconciliationRecordDone(id, remark));
    }

    AdminPayReconciliationRecordView toPayReconciliationRecordView(PayReconciliationRecord record) {
        String diffCategory = AdminReconciliationPolicy.category(record);
        return new AdminPayReconciliationRecordView(
                record.id(),
                record.batchNo(),
                record.bizType(),
                record.orderNo(),
                record.payOrderNo(),
                record.refundNo(),
                record.localStatus(),
                record.channelStatus(),
                record.localAmountCent(),
                record.channelAmountCent(),
                record.consistent(),
                record.diffType(),
                record.repairStatus(),
                record.remark(),
                diffCategory,
                AdminReconciliationPolicy.categoryLabel(diffCategory),
                AdminReconciliationPolicy.suggestion(record),
                AdminReconciliationPolicy.actions(record),
                record.repairedAt(),
                record.createdAt());
    }

    public PageResult<AdminAftersaleSummaryView> adminAftersales(String status, String keyword, Long userId, long page, long size) {
        requireAdmin();
        String safeKeyword = blank(keyword) ? null : keyword.trim();
        String safeStatus = blank(status) ? null : status.trim();
        List<AdminAftersaleSummaryView> rows = aftersaleFacade.findAll().stream()
                .filter(item -> safeStatus == null || safeStatus.equalsIgnoreCase(item.status()))
                .filter(item -> userId == null || userId.equals(item.userId()))
                .filter(item -> safeKeyword == null || contains(item.aftersaleNo(), safeKeyword) || contains(item.orderNo(), safeKeyword) || contains(String.valueOf(item.userId()), safeKeyword))
                .map(this::toAftersaleSummary)
                .toList();
        return PageResult.of(rows, page, size);
    }

    public AdminAftersaleDetailView adminAftersaleDetail(String aftersaleNo) {
        requireAdmin();
        return toAftersaleDetail(aftersaleFacade.getByAftersaleNo(aftersaleNo));
    }

    private AdminAftersaleSummaryView toAftersaleSummary(AftersaleOrder order) {
        return new AdminAftersaleSummaryView(order.id(), order.aftersaleNo(), order.orderNo(), order.userId(), order.aftersaleType(), order.status(), order.refundAmountCent(), order.reason(), order.rejectReason(), order.failReason(), order.createdAt());
    }

    private AdminAftersaleDetailView toAftersaleDetail(AftersaleOrder order) {
        return new AdminAftersaleDetailView(order.id(), order.aftersaleNo(), order.orderNo(), order.userId(), order.aftersaleType(), order.status(), order.refundAmountCent(), order.reason(), order.rejectReason(), order.failReason(), order.createdAt(), order.updatedAt());
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
        return PageResult.of(sortList(rows, reconcileComparator(sortBy), blank(sortOrder) ? "desc" : sortOrder), page, size);
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
        return buildDashboardOverview();
    }

    public Map<String, Long> financeCumulativeNetIncome() {
        requireAdmin();
        long paidAmountCent = financePaidAmountCent(null, null);
        long refundAmountCent = financeRefundAmountCent(null, null);
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        long monthPaidAmountCent = financePaidAmountCent(monthStart, today);
        long monthRefundAmountCent = financeRefundAmountCent(monthStart, today);
        return Map.of(
                "cumulativePaidAmountCent", paidAmountCent,
                "cumulativeRefundAmountCent", refundAmountCent,
                "cumulativeNetIncomeCent", paidAmountCent - refundAmountCent,
                "monthPaidAmountCent", monthPaidAmountCent,
                "monthRefundAmountCent", monthRefundAmountCent,
                "monthNetIncomeCent", monthPaidAmountCent - monthRefundAmountCent
        );
    }

    public List<AdminDashboardFinanceTrendView> financeTrend() {
        requireAdmin();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        Map<LocalDate, Long> paidAmountByDate = queryAmountByDate(
                "SELECT DATE(created_at) AS biz_date, COALESCE(SUM(pay_amount_cent), 0) AS amount "
                        + "FROM pay_order "
                        + "WHERE pay_status IN ('SUCCESS', 'REFUND_PENDING', 'REFUNDING', 'PARTIALLY_REFUNDED', 'REFUND_FAILED') "
                        + "AND DATE(created_at) BETWEEN ? AND ? "
                        + "GROUP BY DATE(created_at)",
                startDate,
                endDate
        );
        Map<LocalDate, Long> refundAmountByDate = queryAmountByDate(
                "SELECT DATE(COALESCE(success_at, updated_at, created_at)) AS biz_date, COALESCE(SUM(refund_amount_cent), 0) AS amount "
                        + "FROM pay_refund_order "
                        + "WHERE refund_status = 'REFUND_SUCCESS' "
                        + "AND DATE(COALESCE(success_at, updated_at, created_at)) BETWEEN ? AND ? "
                        + "GROUP BY DATE(COALESCE(success_at, updated_at, created_at))",
                startDate,
                endDate
        );
        Map<LocalDate, Long> pendingDiffByDate = queryAmountByDate(
                "SELECT reconcile_date AS biz_date, COALESCE(SUM(pending_count), 0) AS amount "
                        + "FROM pay_reconcile_task "
                        + "WHERE reconcile_date BETWEEN ? AND ? AND COALESCE(pending_count, 0) > 0 "
                        + "GROUP BY reconcile_date",
                startDate,
                endDate
        );
        List<AdminDashboardFinanceTrendView> rows = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            long paidAmountCent = paidAmountByDate.getOrDefault(date, 0L);
            long refundAmountCent = refundAmountByDate.getOrDefault(date, 0L);
            rows.add(new AdminDashboardFinanceTrendView(
                    date.toString(),
                    paidAmountCent,
                    refundAmountCent,
                    paidAmountCent - refundAmountCent,
                    pendingDiffByDate.getOrDefault(date, 0L)
            ));
        }
        return rows;
    }

    public List<AdminDashboardWarehouseTrendView> warehouseTrend() {
        requireAdmin();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        Map<LocalDate, Long> shippedItemCountByDate = queryAmountByDate(
                "SELECT DATE(o.shipped_at) AS biz_date, COALESCE(SUM(oi.quantity), 0) AS amount "
                        + "FROM oms_order o "
                        + "JOIN oms_order_item oi ON oi.order_id = o.id "
                        + "WHERE o.shipped_at IS NOT NULL "
                        + "AND DATE(o.shipped_at) BETWEEN ? AND ? "
                        + "AND o.deleted_at IS NULL "
                        + "GROUP BY DATE(o.shipped_at)",
                startDate,
                endDate
        );
        Map<LocalDate, Long> shippedOrderCountByDate = queryAmountByDate(
                "SELECT DATE(shipped_at) AS biz_date, COUNT(*) AS amount "
                        + "FROM oms_order "
                        + "WHERE shipped_at IS NOT NULL "
                        + "AND DATE(shipped_at) BETWEEN ? AND ? "
                        + "AND deleted_at IS NULL "
                        + "GROUP BY DATE(shipped_at)",
                startDate,
                endDate
        );
        Map<LocalDate, Long> stockPolicyUpdateCountByDate = queryAmountByDate(
                "SELECT DATE(created_at) AS biz_date, COUNT(*) AS amount "
                        + "FROM ums_admin_operation_log "
                        + "WHERE DATE(created_at) BETWEEN ? AND ? "
                        + "AND operation_module = 'STOCK' "
                        + "AND operation_type = 'STOCK_POLICY_UPDATE' "
                        + "AND operation_result = 'SUCCESS' "
                        + "GROUP BY DATE(created_at)",
                startDate,
                endDate
        );
        List<AdminDashboardWarehouseTrendView> rows = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            rows.add(new AdminDashboardWarehouseTrendView(
                    date.toString(),
                    shippedItemCountByDate.getOrDefault(date, 0L),
                    shippedOrderCountByDate.getOrDefault(date, 0L),
                    stockPolicyUpdateCountByDate.getOrDefault(date, 0L)
            ));
        }
        return rows;
    }

    private long financePaidAmountCent(LocalDate startDate, LocalDate endDate) {
        String dateCondition = startDate == null || endDate == null ? "" : " AND DATE(created_at) BETWEEN ? AND ?";
        String sql = "SELECT COALESCE(SUM(pay_amount_cent), 0) AS amount "
                + "FROM pay_order "
                + "WHERE pay_status IN ('SUCCESS', 'REFUND_PENDING', 'REFUNDING', 'PARTIALLY_REFUNDED', 'REFUND_FAILED')"
                + dateCondition;
        Long amount = startDate == null || endDate == null
                ? jdbcTemplate.queryForObject(sql, Long.class)
                : jdbcTemplate.queryForObject(sql, Long.class, startDate, endDate);
        return amount == null ? 0L : amount;
    }

    private long financeRefundAmountCent(LocalDate startDate, LocalDate endDate) {
        String dateCondition = startDate == null || endDate == null ? "" : " AND DATE(COALESCE(success_at, updated_at, created_at)) BETWEEN ? AND ?";
        String sql = "SELECT COALESCE(SUM(refund_amount_cent), 0) AS amount "
                + "FROM pay_refund_order "
                + "WHERE refund_status = 'REFUND_SUCCESS'"
                + dateCondition;
        Long amount = startDate == null || endDate == null
                ? jdbcTemplate.queryForObject(sql, Long.class)
                : jdbcTemplate.queryForObject(sql, Long.class, startDate, endDate);
        return amount == null ? 0L : amount;
    }

    private long queryAmount(String sql) {
        Long amount = jdbcTemplate.queryForObject(sql, Long.class);
        return amount == null ? 0L : amount;
    }

    private Map<LocalDate, Long> queryAmountByDate(String sql, LocalDate startDate, LocalDate endDate) {
        return jdbcTemplate.query(sql, (rs) -> {
            Map<LocalDate, Long> result = new java.util.HashMap<>();
            while (rs.next()) {
                Object rawDate = rs.getObject("biz_date");
                LocalDate date;
                if (rawDate instanceof java.sql.Date sqlDate) {
                    date = sqlDate.toLocalDate();
                } else if (rawDate instanceof LocalDate localDate) {
                    date = localDate;
                } else {
                    date = LocalDate.parse(String.valueOf(rawDate).substring(0, 10));
                }
                result.put(date, rs.getLong("amount"));
            }
            return result;
        }, startDate, endDate);
    }

    private AdminDashboardOverviewView buildDashboardOverview() {
        List<Order> orders = orderFacade.findAll();
        List<com.mallfei.pay.domain.model.PayOrder> pays = payFacade.findAll();
        Map<String, PayOrder> payByOrderNo = pays.stream()
                .collect(Collectors.toMap(PayOrder::orderNo, Function.identity(), (left, right) -> left));
        long todayOrderCount = orders.stream()
                .map(Order::createdAt)
                .filter(java.util.Objects::nonNull)
                .filter(createdAt -> createdAt.toLocalDate().equals(java.time.LocalDate.now()))
                .count();
        AdminDashboardStatsView stats = new AdminDashboardStatsView(
                orderFacade.countAll(),
                todayOrderCount,
                orderFacade.countByStatus(Order.STATUS_PENDING_PAYMENT),
                orderFacade.countByStatus(Order.STATUS_PAID),
                orderFacade.countByStatus(Order.STATUS_SHIPPED),
                orderFacade.countByStatus(Order.STATUS_COMPLETED),
                orderFacade.countCancelled(),
                orderFacade.countByStatus(Order.STATUS_PAYMENT_EXCEPTION),
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
        long payAbnormalReconcileCount = payFacade.countReconciliationRecords(null, Boolean.FALSE, PayReconciliationRecord.REPAIR_PENDING);
        long stockAbnormalReconcileCount = stockFacade.pageReconciliationRecords(null, "INCONSISTENT", 1, 1, "id", "desc").total();
        long abnormalReconcileCount = payAbnormalReconcileCount + stockAbnormalReconcileCount;
        long stockWarningCount = warningStats.lowCount() + warningStats.highCount();
        List<AdminStockOperationLogView> recentStockOperationLogs = recentStockOperationLogs();
        AdminProductOperationStatsView productStats = buildProductOperationStats();
        List<AdminProductSalesMonthlyTrendView> productSalesMonthlyTrend = productFacade.recentMonthlySalesTrend(6).stream()
                .map(item -> new AdminProductSalesMonthlyTrendView(item.month(), item.quantity(), item.amountCent()))
                .toList();
        long paymentExceptionCount = orders.stream()
                .filter(order -> Order.STATUS_PAYMENT_EXCEPTION.equals(order.orderStatus()))
                .count();
        List<AftersaleOrder> aftersaleOrders = aftersaleFacade.findAll();
        long aftersalePendingReviewCount = aftersaleOrders.stream()
                .filter(order -> AftersaleOrder.STATUS_PENDING_REVIEW.equals(order.status()))
                .count();
        long aftersaleProcessingCount = aftersaleOrders.stream()
                .filter(order -> AftersaleOrder.STATUS_APPROVED.equals(order.status()) || AftersaleOrder.STATUS_REFUND_PROCESSING.equals(order.status()))
                .count();
        long refundFailedCount = aftersaleOrders.stream()
                .filter(order -> AftersaleOrder.STATUS_REFUND_FAILED.equals(order.status()))
                .count();
        List<AdminDashboardTodoView> todos = new ArrayList<>();
        todos.add(new AdminDashboardTodoView("order:pending-payment", "待支付订单", "需要关注支付转化", stats.pendingOrderCount(), stats.pendingOrderCount() > 0 ? "warning" : "info", "/orders", "status", "PENDING_PAYMENT"));
        todos.add(new AdminDashboardTodoView("order:paid-unshipped", "待发货订单", "已支付待履约，需尽快安排发货", stats.paidOrderCount(), stats.paidOrderCount() > 0 ? "warning" : "info", "/orders", "status", "PAID"));
        todos.add(new AdminDashboardTodoView("order:shipped-confirm", "待确认收货", "已发货订单需关注签收与自动确认", stats.shippedOrderCount(), stats.shippedOrderCount() > 0 ? "info" : "info", "/orders", "status", "SHIPPED"));
        todos.add(new AdminDashboardTodoView("aftersale:pending-review", "售后待审核", "用户售后申请需要运营判断是否通过", aftersalePendingReviewCount, aftersalePendingReviewCount > 0 ? "warning" : "info", "/aftersales", "status", "PENDING_REVIEW"));
        todos.add(new AdminDashboardTodoView("aftersale:processing", "售后处理中", "已通过售后需跟进退款/处理进度", aftersaleProcessingCount, aftersaleProcessingCount > 0 ? "warning" : "info", "/aftersales", "status", "APPROVED"));
        todos.add(new AdminDashboardTodoView("order:payment-exception", "订单支付异常", "订单支付状态需人工核验或转支付同步", paymentExceptionCount, paymentExceptionCount > 0 ? "danger" : "info", "/orders", "status", "PAYMENT_EXCEPTION"));
        todos.add(new AdminDashboardTodoView("pay:pending", "待处理支付单", "建议尽快核查支付流转", stats.pendingPayCount(), stats.pendingPayCount() > 0 ? "warning" : "info", "/pays", "status", "PENDING"));
        todos.add(new AdminDashboardTodoView("refund:failed", "退款失败", "渠道退款失败需财务核查并同步处理", refundFailedCount, refundFailedCount > 0 ? "danger" : "info", "/pays/refunds", "status", "REFUND_FAILED"));
        todos.add(new AdminDashboardTodoView("reconcile:pay-abnormal", "支付对账异常", "订单与支付单金额或状态不一致", payAbnormalReconcileCount, payAbnormalReconcileCount > 0 ? "danger" : "info", "/reconciliations", "status", "ABNORMAL"));
        todos.add(new AdminDashboardTodoView("reconcile:stock-abnormal", "库存对账异常", "库存快照与预占记录存在差异", stockAbnormalReconcileCount, stockAbnormalReconcileCount > 0 ? "danger" : "info", "/reconciliations", "tab", "stock"));
        todos.add(new AdminDashboardTodoView("stock:low-warning", "低库存预警", "存在补货压力 SKU", warningStats.lowCount(), warningStats.lowCount() > 0 ? "warning" : "info", "/stocks", "warningStatus", "LOW"));
        todos.add(new AdminDashboardTodoView("stock:high-warning", "高库存预警", "存在库存积压风险 SKU", warningStats.highCount(), warningStats.highCount() > 0 ? "warning" : "info", "/stocks", "warningStatus", "HIGH"));
        List<AdminAftersaleSummaryView> pendingAftersales = aftersaleOrders.stream()
                .filter(order -> AftersaleOrder.STATUS_PENDING_REVIEW.equals(order.status()))
                .sorted(Comparator.comparing(AftersaleOrder::createdAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .limit(5)
                .map(this::toAftersaleSummary)
                .toList();
        List<AdminDashboardShortcutView> shortcuts = List.of(
                new AdminDashboardShortcutView("待支付订单", "查看未完成支付的订单", "/orders", "status", "PENDING_PAYMENT", stats.pendingOrderCount()),
                new AdminDashboardShortcutView("待处理支付", "查看待处理支付单", "/pays", "status", "PENDING", stats.pendingPayCount()),
                new AdminDashboardShortcutView("异常对账", "支付 " + payAbnormalReconcileCount + " / 库存 " + stockAbnormalReconcileCount, "/reconciliations", "status", "ABNORMAL", abnormalReconcileCount),
                new AdminDashboardShortcutView("库存预警", "低 " + warningStats.lowCount() + " / 高 " + warningStats.highCount(), "/stocks", "warningStatus", "LOW", stockWarningCount)
        );
        List<AdminDashboardOperationsTrendView> operationsTrend = buildOperationsTrend(orders, aftersaleOrders);
        return adminViewAssembler.toDashboardOverview(stats, warningStats, recentStockOperationLogs, pendingAftersales, productStats, productSalesMonthlyTrend, operationsTrend, abnormalReconcileCount, payAbnormalReconcileCount, stockAbnormalReconcileCount, todos, shortcuts);
    }

    private List<AdminDashboardOperationsTrendView> buildOperationsTrend(List<Order> orders, List<AftersaleOrder> aftersaleOrders) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        Map<LocalDate, Long> orderCountByDate = orders.stream()
                .filter(order -> order.createdAt() != null)
                .map(order -> order.createdAt().toLocalDate())
                .filter(date -> !date.isBefore(startDate) && !date.isAfter(endDate))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Map<LocalDate, Long> completedOrderCountByDate = orders.stream()
                .filter(order -> order.createdAt() != null)
                .filter(order -> Order.STATUS_COMPLETED.equals(order.orderStatus()))
                .map(order -> order.createdAt().toLocalDate())
                .filter(date -> !date.isBefore(startDate) && !date.isAfter(endDate))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Map<LocalDate, Long> aftersaleCountByDate = aftersaleOrders.stream()
                .filter(order -> order.createdAt() != null)
                .map(order -> order.createdAt().toLocalDate())
                .filter(date -> !date.isBefore(startDate) && !date.isAfter(endDate))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<AdminDashboardOperationsTrendView> rows = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            rows.add(new AdminDashboardOperationsTrendView(
                    date.toString(),
                    orderCountByDate.getOrDefault(date, 0L),
                    completedOrderCountByDate.getOrDefault(date, 0L),
                    aftersaleCountByDate.getOrDefault(date, 0L)
            ));
        }
        return rows;
    }

    private List<AdminStockOperationLogView> recentStockOperationLogs() {
        PageResult<StockOperationLogSnapshot> result = stockFacade.pageLogs(null, null, null, null, 1, 1000);
        return result.records().stream()
                .map(this::withSkuName)
                .map(adminViewAssembler::toStockOperationLogView)
                .sorted(stockLogComparator("createdAt").reversed())
                .limit(5)
                .toList();
    }

    private AdminProductOperationStatsView buildProductOperationStats() {
        AdminProductSalesThresholdConfigView thresholdConfig = operationConfigApplicationService.productSalesThresholdConfig();
        int hotSellingThreshold = thresholdConfig.hotSalesThreshold();
        int lowSellingThreshold = thresholdConfig.lowSalesThreshold();
        List<ProductSpu> products = productFacade.findAll();
        Map<Long, com.mallfei.product.application.service.ProductSalesStatApplicationService.ProductSalesAggregate> recent30DaySalesBySpu = productFacade.recent30DaySalesBySpuIds(products.stream().map(ProductSpu::id).toList());
        long onlineCount = products.stream().filter(product -> "ONLINE".equalsIgnoreCase(product.status())).count();
        long pendingOnlineCount = products.stream().filter(product -> !"ONLINE".equalsIgnoreCase(product.status())).count();
        long hotSellingCount = products.stream()
                .filter(product -> "ONLINE".equalsIgnoreCase(product.status()))
                .filter(product -> monthlySales(product, recent30DaySalesBySpu) >= hotSellingThreshold)
                .count();
        long lowSellingCount = products.stream()
                .filter(product -> "ONLINE".equalsIgnoreCase(product.status()))
                .filter(product -> monthlySales(product, recent30DaySalesBySpu) <= lowSellingThreshold)
                .count();
        return new AdminProductOperationStatsView(products.size(), onlineCount, pendingOnlineCount, hotSellingCount, lowSellingCount, productFacade.recent7DaySalesCount(), productFacade.recent30DaySalesCount(), productFacade.currentMonthSalesCount(), productFacade.recent30DaySalesAmountCent());
    }

    private int monthlySales(ProductSpu product, Map<Long, com.mallfei.product.application.service.ProductSalesStatApplicationService.ProductSalesAggregate> recent30DaySalesBySpu) {
        com.mallfei.product.application.service.ProductSalesStatApplicationService.ProductSalesAggregate aggregate = recent30DaySalesBySpu.get(product.id());
        return aggregate == null ? 0 : aggregate.quantity();
    }

    public PageResult<StockSnapshot> stockList(Long skuId, String stockStatus, String warningStatus, long page, long size, String sortBy, String sortOrder) {
        requireAdmin();
        PageResult<StockSnapshot> result = stockFacade.stockList(new StockQuery(skuId, stockStatus, warningStatus, blank(sortBy) ? "skuId" : sortBy, blank(sortOrder) ? "asc" : sortOrder, page, size));
        return new PageResult<>(result.page(), result.size(), result.total(), result.pages(), result.records().stream().map(this::withSkuName).toList());
    }

    public PageResult<AdminTodayActiveStockView> todayActiveStockList(Long skuId, String stockStatus, String warningStatus, LocalDate stockDate, Long currentTimestamp, long page, long size, String sortBy, String sortOrder) {
        requireAdmin();
        long actualPage = Math.max(1, page);
        long actualSize = Math.max(1, Math.min(size, 200));
        String orderBy = stockOrderBy(sortBy);
        String orderDirection = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";
        StringBuilder logWhere = new StringBuilder(" WHERE l.created_at >= ? AND l.created_at < ?");
        StringBuilder stockWhere = new StringBuilder();
        List<Object> args = new ArrayList<>();
        LocalDate actualStockDate = stockDate != null ? stockDate : (currentTimestamp == null ? LocalDate.now() : java.time.Instant.ofEpochMilli(currentTimestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        LocalDateTime startTime = actualStockDate.minusDays(2).atStartOfDay();
        LocalDateTime endTime = actualStockDate.plusDays(1).atStartOfDay();
        args.add(startTime);
        args.add(endTime);
        if (skuId != null) {
            stockWhere.append(" AND s.sku_id = ?");
            args.add(skuId);
        }
        if (!blank(stockStatus)) {
            stockWhere.append(" AND s.stock_status = ?");
            args.add(stockStatus.trim().toUpperCase());
        }
        if (!blank(warningStatus)) {
            stockWhere.append(" AND s.warning_status = ?");
            args.add(warningStatus.trim().toUpperCase());
        }
        String baseFrom = " FROM ims_stock s JOIN (" +
                " SELECT l.sku_id, MAX(l.created_at) AS latest_stock_time" +
                " FROM ims_stock_operation_log l" + logWhere +
                " GROUP BY l.sku_id" +
                ") recent ON recent.sku_id = s.sku_id" +
                " LEFT JOIN pms_sku sku ON sku.id = s.sku_id" +
                " LEFT JOIN pms_spu spu ON spu.id = sku.spu_id" +
                " LEFT JOIN pms_category cat ON cat.id = spu.category_id" +
                " WHERE 1=1" + stockWhere;
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1)" + baseFrom, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(actualSize);
        pageArgs.add((actualPage - 1) * actualSize);
        List<AdminTodayActiveStockView> rows = jdbcTemplate.query("SELECT s.sku_id, COALESCE(sku.sku_name, spu.name) AS sku_name, sku.spu_id, spu.category_id, cat.name AS category_name, COALESCE(pt.name, cat.name) AS product_type_name, s.total_stock, s.locked_stock, s.available_stock, s.stock_status, s.low_stock_threshold, s.high_stock_threshold, s.warning_status, recent.latest_stock_time" +
                        " FROM ims_stock s JOIN (" +
                        " SELECT l.sku_id, MAX(l.created_at) AS latest_stock_time" +
                        " FROM ims_stock_operation_log l" + logWhere +
                        " GROUP BY l.sku_id" +
                        ") recent ON recent.sku_id = s.sku_id" +
                        " LEFT JOIN pms_sku sku ON sku.id = s.sku_id" +
                        " LEFT JOIN pms_spu spu ON spu.id = sku.spu_id" +
                        " LEFT JOIN pms_category cat ON cat.id = spu.category_id" +
                        " LEFT JOIN pms_category pt ON pt.id = cat.parent_id" +
                        " WHERE 1=1" + stockWhere +
                        " ORDER BY " + orderBy + " " + orderDirection + " LIMIT ? OFFSET ?",
                (rs, rowNum) -> new AdminTodayActiveStockView(
                        rs.getLong("sku_id"),
                        rs.getString("sku_name"),
                        rs.getObject("spu_id", Long.class),
                        rs.getObject("category_id", Long.class),
                        rs.getString("category_name"),
                        rs.getString("product_type_name"),
                        rs.getInt("total_stock"),
                        rs.getInt("locked_stock"),
                        rs.getInt("available_stock"),
                        rs.getString("stock_status"),
                        rs.getInt("low_stock_threshold"),
                        rs.getInt("high_stock_threshold"),
                        rs.getString("warning_status"),
                        rs.getTimestamp("latest_stock_time") == null ? null : rs.getTimestamp("latest_stock_time").toLocalDateTime().toString(),
                        "TODAY_STOCK_LOG"
                ), pageArgs.toArray());
        long actualTotal = total == null ? 0 : total;
        long pages = (actualTotal + actualSize - 1) / actualSize;
        return new PageResult<>(actualPage, actualSize, actualTotal, pages, rows);
    }

    public PageResult<StockSnapshot> warningStocks(long page, long size) {
        requireAdmin();
        long actualPage = Math.max(1, page);
        long actualSize = Math.max(1, size);
        PageResult<StockSnapshot> lowResult = stockFacade.stockList(new StockQuery(null, null, "LOW", "skuId", "asc", 1, 1000));
        PageResult<StockSnapshot> highResult = stockFacade.stockList(new StockQuery(null, null, "HIGH", "skuId", "asc", 1, 1000));
        List<StockSnapshot> rows = new ArrayList<>();
        rows.addAll(lowResult.records());
        rows.addAll(highResult.records());
        List<StockSnapshot> sorted = rows.stream()
                .sorted(Comparator
                        .comparingInt((StockSnapshot item) -> "LOW".equals(item.warningStatus()) ? 0 : 1)
                        .thenComparing(StockSnapshot::skuId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        long from = Math.min((actualPage - 1) * actualSize, sorted.size());
        long to = Math.min(from + actualSize, sorted.size());
        List<StockSnapshot> pageRows = sorted.subList((int) from, (int) to).stream().map(this::withSkuName).toList();
        long total = sorted.size();
        long pages = (total + actualSize - 1) / actualSize;
        return new PageResult<>(actualPage, actualSize, total, pages, pageRows);
    }

    public PageResult<AdminStockOperationLogView> stockLogs(Long skuId, String operationType, String startTime, String endTime, long page, long size, String sortBy, String sortOrder) {
        requireAdmin();
        LocalDateTime start = blank(startTime) ? null : LocalDateTime.parse(startTime);
        LocalDateTime end = blank(endTime) ? null : LocalDateTime.parse(endTime);
        PageResult<StockOperationLogSnapshot> result = stockFacade.pageLogs(skuId, operationType, start, end, 1, 1000);
        List<AdminStockOperationLogView> rows = result.records().stream().map(this::withSkuName).map(adminViewAssembler::toStockOperationLogView).toList();
        return PageResult.of(sortList(rows, stockLogComparator(sortBy), blank(sortOrder) ? "desc" : sortOrder), page, size);
    }

    private AdminReconcileRowView reconcileRow(Order order, PayOrder pay) {
        boolean amountOk = payOrderDomainService.reconcileAmount(order, pay);
        boolean statusOk = payOrderDomainService.reconcileStatus(order, pay);
        return adminViewAssembler.toReconcileRow(order, pay, amountOk, statusOk);
    }

    private StockSnapshot withSkuName(StockSnapshot stock) {
        return new StockSnapshot(stock.skuId(), skuName(stock.skuId()), stock.totalStock(), stock.lockedStock(), stock.availableStock(), stock.stockStatus(), stock.lowStockThreshold(), stock.highStockThreshold(), stock.warningStatus(), stock.source());
    }

    private StockOperationLogSnapshot withSkuName(StockOperationLogSnapshot log) {
        return new StockOperationLogSnapshot(log.id(), log.skuId(), skuName(log.skuId()), log.operationType(), log.businessType(), log.businessNo(), log.changeQuantity(), log.beforeTotalStock(), log.beforeLockedStock(), log.beforeAvailableStock(), log.afterTotalStock(), log.afterLockedStock(), log.afterAvailableStock(), log.remark(), log.operatorType(), log.operatorId(), log.operatorName(), log.sourceType(), log.createdAt());
    }

    private String skuName(Long skuId) {
        if (skuId == null) {
            return null;
        }
        try {
            return productFacade.getSkuSnapshot(skuId).skuName();
        } catch (Exception ignored) {
            return null;
        }
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

    private String stockOrderBy(String sortBy) {
        String actualSortBy = blank(sortBy) ? "latestStockTime" : sortBy;
        return switch (actualSortBy) {
            case "skuId" -> "s.sku_id";
            case "availableStock" -> "s.available_stock";
            case "lockedStock" -> "s.locked_stock";
            case "totalStock" -> "s.total_stock";
            case "stockStatus" -> "s.stock_status";
            case "warningStatus" -> "s.warning_status";
            case "lowStockThreshold" -> "s.low_stock_threshold";
            case "highStockThreshold" -> "s.high_stock_threshold";
            case "latestStockTime" -> "recent.latest_stock_time";
            default -> "recent.latest_stock_time";
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
            case "skuName" -> Comparator.comparing(AdminStockOperationLogView::skuName, Comparator.nullsLast(String::compareTo));
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

    private boolean contains(String value, String keyword) {
        return value != null && keyword != null && value.toLowerCase().contains(keyword.toLowerCase());
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
