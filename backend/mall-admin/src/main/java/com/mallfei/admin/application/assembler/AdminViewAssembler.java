package com.mallfei.admin.application.assembler;

import com.mallfei.admin.application.vo.AdminAftersaleSummaryView;
import com.mallfei.admin.application.vo.AdminDashboardOperationsTrendView;
import com.mallfei.admin.application.vo.AdminDashboardOverviewView;
import com.mallfei.admin.application.vo.AdminDashboardShortcutView;
import com.mallfei.admin.application.vo.AdminDashboardStatsView;
import com.mallfei.admin.application.vo.AdminDashboardTodoView;
import com.mallfei.admin.application.vo.AdminOrderDetailView;
import com.mallfei.admin.application.vo.AdminOrderItemView;
import com.mallfei.admin.application.vo.AdminOrderSummaryView;
import com.mallfei.admin.application.vo.AdminPayDetailView;
import com.mallfei.admin.application.vo.AdminPaySummaryView;
import com.mallfei.admin.application.vo.AdminProductOperationStatsView;
import com.mallfei.admin.application.vo.AdminProductSalesMonthlyTrendView;
import com.mallfei.admin.application.vo.AdminReconcileRowView;
import com.mallfei.admin.application.vo.AdminStockOperationLogView;
import com.mallfei.admin.application.vo.AdminStockWarningStatsView;
import com.mallfei.order.domain.model.Order;
import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.stock.facade.StockOperationLogSnapshot;
import com.mallfei.stock.facade.StockSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminViewAssembler {

    public AdminOrderSummaryView toOrderSummary(Order order) { return toOrderSummary(order, null, null); }
    public AdminOrderSummaryView toOrderSummary(Order order, String pendingAction, String pendingActionLabel) { return new AdminOrderSummaryView(order.id(), order.orderNo(), order.userId(), order.orderStatus(), order.totalAmountCent(), order.payAmountCent(), order.receiverName(), order.receiverPhone(), order.itemCount(), order.createdAt(), pendingAction, pendingActionLabel); }
    public AdminOrderDetailView toOrderDetail(Order order) { return new AdminOrderDetailView(order.id(), order.orderNo(), order.userId(), order.orderStatus(), order.totalAmountCent(), order.payAmountCent(), order.receiverName(), order.itemCount(), order.receiverPhone(), order.receiverProvinceName(), order.receiverCityName(), order.receiverDistrictName(), order.receiverDetailAddress(), text(order.receiverProvinceName()) + text(order.receiverCityName()) + text(order.receiverDistrictName()) + text(order.receiverDetailAddress()), order.remark(), order.paidAt(), order.cancelledAt(), order.shippedAt(), order.completedAt(), order.items().stream().map(item -> new AdminOrderItemView(item.id(), item.skuId(), item.skuName(), item.quantity(), item.salePriceCent(), item.totalAmountCent())).toList()); }
    public AdminPaySummaryView toPaySummary(PayOrder payOrder) { return new AdminPaySummaryView(payOrder.id(), payOrder.payOrderNo(), payOrder.orderNo(), payOrder.userId(), payOrder.payStatus(), payOrder.payAmountCent(), payOrder.payChannel(), payOrder.createdAt() == null ? "" : payOrder.createdAt().toString()); }
    public AdminPayDetailView toPayDetail(PayOrder payOrder) { return new AdminPayDetailView(payOrder.payOrderNo(), payOrder.orderNo(), payOrder.userId(), payOrder.payStatus(), payOrder.payAmountCent(), payOrder.payChannel(), payOrder.transactionNo(), payOrder.idempotentKey(), payOrder.callbackPayload()); }
    public AdminReconcileRowView toReconcileRow(Order order, PayOrder pay, boolean amountOk, boolean statusOk) { return new AdminReconcileRowView(order.id(), order.orderNo(), order.orderStatus(), order.payAmountCent(), pay != null, pay == null ? null : pay.payOrderNo(), pay == null ? null : pay.payStatus(), pay == null ? null : pay.payAmountCent(), amountOk, statusOk, amountOk && statusOk ? "CONSISTENT" : "ABNORMAL"); }
    public AdminDashboardStatsView toDashboardStats(List<Order> orders, List<PayOrder> pays) { return new AdminDashboardStatsView(orders.size(), orders.stream().map(Order::createdAt).filter(java.util.Objects::nonNull).filter(createdAt -> createdAt.toLocalDate().equals(java.time.LocalDate.now())).count(), countOrders(orders, Order.STATUS_PENDING_PAYMENT), countOrders(orders, Order.STATUS_PAID), countOrders(orders, Order.STATUS_SHIPPED), countOrders(orders, Order.STATUS_COMPLETED), orders.stream().filter(Order::cancelled).count(), countOrders(orders, Order.STATUS_PAYMENT_EXCEPTION), orders.stream().filter(Order::paidOrAfter).mapToLong(Order::safePayAmountCent).sum(), pays.size(), pays.stream().filter(PayOrder::pending).count(), pays.stream().filter(PayOrder::success).count(), pays.stream().filter(PayOrder::closed).count()); }
    public AdminStockOperationLogView toStockOperationLogView(StockOperationLogSnapshot log) { return new AdminStockOperationLogView(log.id(), log.skuId(), log.skuName(), log.operationType(), log.businessType(), log.businessNo(), log.changeQuantity(), log.beforeTotalStock(), log.beforeLockedStock(), log.beforeAvailableStock(), log.afterTotalStock(), log.afterLockedStock(), log.afterAvailableStock(), log.remark(), log.operatorType(), log.operatorId(), log.operatorName(), log.sourceType(), log.createdAt() == null ? null : log.createdAt().toString()); }
    public AdminStockWarningStatsView toStockWarningStats(List<StockSnapshot> stocks) { long low = stocks.stream().filter(item -> "LOW".equals(item.warningStatus())).count(); long high = stocks.stream().filter(item -> "HIGH".equals(item.warningStatus())).count(); long normal = stocks.stream().filter(item -> "NORMAL".equals(item.warningStatus())).count(); return new AdminStockWarningStatsView(low, high, normal, stocks.size()); }
    public AdminDashboardOverviewView toDashboardOverview(AdminDashboardStatsView stats, AdminStockWarningStatsView stockWarningStats, List<AdminStockOperationLogView> recentStockOperationLogs, List<AdminAftersaleSummaryView> pendingAftersales, AdminProductOperationStatsView productStats, List<AdminProductSalesMonthlyTrendView> productSalesMonthlyTrend, List<AdminDashboardOperationsTrendView> operationsTrend, long abnormalReconcileCount, long payAbnormalReconcileCount, long stockAbnormalReconcileCount, List<AdminDashboardTodoView> todos, List<AdminDashboardShortcutView> shortcuts) { return new AdminDashboardOverviewView(stats, stockWarningStats, recentStockOperationLogs, pendingAftersales, productStats, productSalesMonthlyTrend, operationsTrend, abnormalReconcileCount, payAbnormalReconcileCount, stockAbnormalReconcileCount, todos, shortcuts); }
    private long countOrders(List<Order> orders, String status) { return orders.stream().filter(order -> status.equals(order.orderStatus())).count(); }
    private String text(String value) { return value == null ? "" : value; }
}
