package com.mallfei.admin.application.service;

import com.mallfei.admin.application.dto.AdminClosePayOrderRequest;
import com.mallfei.admin.application.dto.AdminReconcileHandleRequest;
import com.mallfei.admin.application.vo.AdminReconcileRowView;
import com.mallfei.pay.application.vo.PayOrderView;
import com.mallfei.pay.application.vo.PayReconcileResultView;
import com.mallfei.pay.facade.PayFacade;
import org.springframework.stereotype.Service;

@Service
public class AdminPayManagementApplicationService {

    private final PayFacade payFacade;
    private final AdminQueryApplicationService adminQueryApplicationService;
    private final AdminAccountManagementApplicationService adminAccountManagementApplicationService;

    public AdminPayManagementApplicationService(PayFacade payFacade,
                                                AdminQueryApplicationService adminQueryApplicationService,
                                                AdminAccountManagementApplicationService adminAccountManagementApplicationService) {
        this.payFacade = payFacade;
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

    public AdminReconcileRowView reconcile(String orderNo) {
        PayReconcileResultView result = payFacade.reconcile(orderNo);
        adminAccountManagementApplicationService.recordOperation("PAY", "PAY_RECONCILE", "执行支付对账 orderNo=" + orderNo, "SUCCESS");
        return adminQueryApplicationService.reconcileRowView(orderNo, result);
    }

    public AdminReconcileRowView handleReconcile(String orderNo, AdminReconcileHandleRequest request) {
        String action = request.action().toUpperCase();
        if ("RETRY_RECONCILE".equals(action)) {
            return reconcile(orderNo);
        }
        if ("CLOSE_PAY_ORDER".equals(action)) {
            payFacade.closePayOrder(orderNo, "RECONCILE_HANDLE:" + request.reason());
            adminAccountManagementApplicationService.recordOperation("PAY", "PAY_RECONCILE_HANDLE", "处理对账异常 orderNo=" + orderNo + "，动作=" + action + "，原因=" + request.reason(), "SUCCESS");
            return adminQueryApplicationService.reconcileRow(orderNo);
        }
        throw com.mallfei.common.exception.BusinessException.badRequest("不支持的对账处理动作");
    }
}
