package com.mallfei.admin.application.service;

import com.mallfei.admin.application.dto.AdminClosePayOrderRequest;
import com.mallfei.admin.application.dto.AdminReconcileHandleRequest;
import com.mallfei.admin.application.vo.AdminPayReconciliationRecordView;
import com.mallfei.admin.application.vo.AdminReconcileRowView;
import com.mallfei.admin.application.vo.AdminReconciliationHandleResultView;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.facade.OrderFacade;
import com.mallfei.pay.application.vo.PayOrderView;
import com.mallfei.pay.application.vo.PayReconcileResultView;
import com.mallfei.pay.domain.model.PayReconciliationRecord;
import com.mallfei.pay.facade.PayFacade;
import org.springframework.stereotype.Service;

@Service
public class AdminPayManagementApplicationService {

    private final PayFacade payFacade;
    private final OrderFacade orderFacade;
    private final AdminQueryApplicationService adminQueryApplicationService;
    private final AdminAccountManagementApplicationService adminAccountManagementApplicationService;

    public AdminPayManagementApplicationService(PayFacade payFacade,
                                                OrderFacade orderFacade,
                                                AdminQueryApplicationService adminQueryApplicationService,
                                                AdminAccountManagementApplicationService adminAccountManagementApplicationService) {
        this.payFacade = payFacade;
        this.orderFacade = orderFacade;
        this.adminQueryApplicationService = adminQueryApplicationService;
        this.adminAccountManagementApplicationService = adminAccountManagementApplicationService;
    }

    public PayOrderView closePayOrder(String orderNo, AdminClosePayOrderRequest request) {
        PayOrderView result = payFacade.closePayOrder(orderNo, request.reason());
        adminAccountManagementApplicationService.recordOperation("PAY", "PAY_CLOSE", "关闭支付单 orderNo=" + orderNo + "，原因=" + request.reason(), "SUCCESS");
        return result;
    }

    public PayOrderView syncOrderStatus(String orderNo) {
        PayOrderView result = payFacade.syncOrderStatus(orderNo);
        adminAccountManagementApplicationService.recordOperation("PAY", "PAY_SYNC", "同步订单支付状态 orderNo=" + orderNo, "SUCCESS");
        return result;
    }

    public PayOrderView repairPaidOrder(String orderNo) {
        PayOrderView result = payFacade.repairPaidOrder(orderNo);
        adminAccountManagementApplicationService.recordOperation("PAY", "PAY_REPAIR", "补偿订单支付状态 orderNo=" + orderNo, "SUCCESS");
        return result;
    }

    public PayOrderView reconcileRefund(String orderNo, String refundNo, Long refundAmountCent) {
        PayOrderView result = payFacade.reconcileRefund(orderNo, refundNo, refundAmountCent);
        adminAccountManagementApplicationService.recordOperation("PAY", "REFUND_SYNC", "同步退款状态 orderNo=" + orderNo + "，refundNo=" + refundNo, "SUCCESS");
        return result;
    }

    public AdminReconcileRowView reconcile(String orderNo) {
        PayReconcileResultView result = payFacade.reconcile(orderNo);
        adminAccountManagementApplicationService.recordOperation("PAY", "PAY_RECONCILE", "执行支付对账 orderNo=" + orderNo, "SUCCESS");
        return adminQueryApplicationService.reconcileRowView(orderNo, result);
    }

    public AdminReconcileRowView handleReconcile(String orderNo, AdminReconcileHandleRequest request) {
        String action = normalizeAction(request.action());
        if (AdminReconciliationPolicy.ACTION_RETRY_RECONCILE.equals(action)) {
            return reconcile(orderNo);
        }
        if ("CLOSE_PAY_ORDER".equals(action)) {
            payFacade.closePayOrder(orderNo, "RECONCILE_HANDLE:" + request.reason());
            adminAccountManagementApplicationService.recordOperation("PAY", "PAY_RECONCILE_HANDLE", "处理对账异常 orderNo=" + orderNo + "，动作=" + action + "，原因=" + request.reason(), "SUCCESS");
            return adminQueryApplicationService.reconcileRow(orderNo);
        }
        throw BusinessException.badRequest("不支持的对账处理动作");
    }

    public AdminReconciliationHandleResultView handlePayReconciliationRecord(Long id, AdminReconcileHandleRequest request) {
        PayReconciliationRecord record = payFacade.getReconciliationRecord(id);
        ensureRecordActionable(record);
        String action = normalizeAction(request.action());
        ensureActionAllowed(record, action);
        String reason = request.reason() == null ? "" : request.reason().trim();
        PayReconciliationRecord handled = switch (action) {
            case AdminReconciliationPolicy.ACTION_RETRY_RECONCILE -> handleRetryReconcile(record, reason);
            case AdminReconciliationPolicy.ACTION_SYNC_PAY_STATUS -> handleSyncPayStatus(record, reason);
            case AdminReconciliationPolicy.ACTION_REPAIR_ORDER_PAID -> handleRepairOrderPaid(record, reason);
            case AdminReconciliationPolicy.ACTION_RESTORE_PENDING_PAYMENT -> handleRestorePendingPayment(record, reason);
            case AdminReconciliationPolicy.ACTION_CLOSE_AND_RELEASE_STOCK -> handleCloseAndRelease(record, reason);
            case AdminReconciliationPolicy.ACTION_WAIT_CHANNEL_CONFIRM -> markRecordStatus(record, PayReconciliationRecord.REPAIR_WAIT_CHANNEL_CONFIRM, "已转渠道调单", reason);
            case AdminReconciliationPolicy.ACTION_WAIT_USER_REPAY -> markRecordStatus(record, PayReconciliationRecord.REPAIR_WAIT_USER_REPAY, "已转用户补款/追缴", reason);
            case AdminReconciliationPolicy.ACTION_WAIT_REFUND -> markRecordStatus(record, PayReconciliationRecord.REPAIR_WAIT_REFUND, "已转待退款处理", reason);
            case AdminReconciliationPolicy.ACTION_MANUAL_REVIEW -> markRecordStatus(record, PayReconciliationRecord.REPAIR_MANUAL_REVIEW, "已转人工审核", reason);
            case AdminReconciliationPolicy.ACTION_MARK_DONE -> payFacade.markReconciliationRecordDone(record.id(), appendRemark(record, "人工确认业务问题已解决", reason));
            case AdminReconciliationPolicy.ACTION_IGNORE -> payFacade.markReconciliationRecordIgnored(record.id(), appendRemark(record, "人工确认忽略归档", reason));
            default -> throw BusinessException.badRequest("不支持的对账处置动作");
        };
        adminAccountManagementApplicationService.recordOperation("PAY", "PAY_RECONCILE_RECORD_HANDLE", "处置对账记录 id=" + id + "，orderNo=" + record.orderNo() + "，动作=" + action + "，原因=" + reason, "SUCCESS");
        AdminPayReconciliationRecordView view = adminQueryApplicationService.toPayReconciliationRecordView(handled);
        return new AdminReconciliationHandleResultView(action, "SUCCESS", handleMessage(action, handled), view);
    }

    private PayReconciliationRecord handleRetryReconcile(PayReconciliationRecord record, String reason) {
        reconcile(record.orderNo());
        return payFacade.markReconciliationRecordStatus(record.id(), PayReconciliationRecord.REPAIR_RECHECKING, appendRemark(record, "已重新发起对账", reason));
    }

    private PayReconciliationRecord handleSyncPayStatus(PayReconciliationRecord record, String reason) {
        payFacade.syncOrderStatus(record.orderNo());
        return payFacade.markReconciliationRecordDone(record.id(), appendRemark(record, "已同步支付状态并尝试补偿订单", reason));
    }

    private PayReconciliationRecord handleRepairOrderPaid(PayReconciliationRecord record, String reason) {
        payFacade.repairPaidOrder(record.orderNo());
        return payFacade.markReconciliationRecordDone(record.id(), appendRemark(record, "已补偿订单为已支付", reason));
    }

    private PayReconciliationRecord handleRestorePendingPayment(PayReconciliationRecord record, String reason) {
        orderFacade.restorePendingPaymentByAdmin(record.orderNo(), appendRemark(record, "对账处置恢复待支付", reason));
        return payFacade.markReconciliationRecordDone(record.id(), appendRemark(record, "已恢复订单待支付", reason));
    }

    private PayReconciliationRecord handleCloseAndRelease(PayReconciliationRecord record, String reason) {
        orderFacade.cancelByAdmin(record.orderNo());
        return payFacade.markReconciliationRecordDone(record.id(), appendRemark(record, "已关闭订单并释放库存", reason));
    }

    private PayReconciliationRecord markRecordStatus(PayReconciliationRecord record, String status, String actionRemark, String reason) {
        return payFacade.markReconciliationRecordStatus(record.id(), status, appendRemark(record, actionRemark, reason));
    }

    private void ensureRecordActionable(PayReconciliationRecord record) {
        if (record.consistent() != null && record.consistent()) {
            throw BusinessException.badRequest("对平记录无需处置");
        }
        if (!PayReconciliationRecord.REPAIR_PENDING.equals(record.repairStatus())) {
            throw BusinessException.badRequest("当前对账记录不是待处理状态，不能重复处置");
        }
    }

    private void ensureActionAllowed(PayReconciliationRecord record, String action) {
        boolean allowed = AdminReconciliationPolicy.actions(record).stream().anyMatch(item -> action.equals(item.action()));
        if (!allowed) {
            throw BusinessException.badRequest("当前差异类型不允许执行该处置动作");
        }
    }

    private String normalizeAction(String action) {
        if (action == null || action.isBlank()) {
            throw BusinessException.badRequest("处理动作不能为空");
        }
        return action.trim().toUpperCase();
    }

    private String appendRemark(PayReconciliationRecord record, String actionRemark, String reason) {
        StringBuilder builder = new StringBuilder();
        if (record.remark() != null && !record.remark().isBlank()) {
            builder.append(record.remark().trim());
        }
        if (!builder.isEmpty()) {
            builder.append("；");
        }
        builder.append(actionRemark);
        if (reason != null && !reason.isBlank()) {
            builder.append("，说明=").append(reason.trim());
        }
        return builder.toString();
    }

    private String handleMessage(String action, PayReconciliationRecord record) {
        return switch (action) {
            case AdminReconciliationPolicy.ACTION_SYNC_PAY_STATUS -> "已执行支付状态同步，最新处理状态=" + record.repairStatus();
            case AdminReconciliationPolicy.ACTION_REPAIR_ORDER_PAID -> "已补偿订单支付状态，最新处理状态=" + record.repairStatus();
            case AdminReconciliationPolicy.ACTION_RESTORE_PENDING_PAYMENT -> "已恢复订单待支付，最新处理状态=" + record.repairStatus();
            case AdminReconciliationPolicy.ACTION_CLOSE_AND_RELEASE_STOCK -> "已关闭订单并释放库存，最新处理状态=" + record.repairStatus();
            default -> "对账处置已提交，最新处理状态=" + record.repairStatus();
        };
    }
}
