package com.mallfei.admin.application.service;

import com.mallfei.admin.application.assembler.AdminViewAssembler;
import com.mallfei.admin.application.dto.AdminOrderExceptionHandleRequest;
import com.mallfei.admin.application.vo.AdminOrderDetailView;
import com.mallfei.admin.application.vo.AdminOrderOperationResultView;
import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.facade.OrderFacade;
import com.mallfei.pay.application.vo.PayOrderView;
import com.mallfei.pay.facade.PayFacade;
import org.springframework.stereotype.Service;

@Service
public class AdminCommandApplicationService {

    private final AuthFacade authFacade;
    private final OrderFacade orderFacade;
    private final PayFacade payFacade;
    private final AdminViewAssembler adminViewAssembler;
    private final AdminAccountManagementApplicationService adminAccountManagementApplicationService;

    public AdminCommandApplicationService(AuthFacade authFacade, OrderFacade orderFacade, PayFacade payFacade, AdminViewAssembler adminViewAssembler, AdminAccountManagementApplicationService adminAccountManagementApplicationService) {
        this.authFacade = authFacade;
        this.orderFacade = orderFacade;
        this.payFacade = payFacade;
        this.adminViewAssembler = adminViewAssembler;
        this.adminAccountManagementApplicationService = adminAccountManagementApplicationService;
    }

    public AdminOrderDetailView cancelOrder(String orderNo) { requireAdmin(); orderFacade.cancelByAdmin(orderNo); adminAccountManagementApplicationService.recordOperation("ORDER", "ORDER_CANCEL", "取消订单：" + orderNo, "SUCCESS"); return adminViewAssembler.toOrderDetail(orderFacade.getByOrderNo(orderNo)); }
    public AdminOrderDetailView shipOrder(String orderNo) { requireAdmin(); orderFacade.shipByAdmin(orderNo); adminAccountManagementApplicationService.recordOperation("ORDER", "ORDER_SHIP", "发货订单：" + orderNo, "SUCCESS"); return adminViewAssembler.toOrderDetail(orderFacade.getByOrderNo(orderNo)); }
    public AdminOrderDetailView completeOrder(String orderNo) { requireAdmin(); orderFacade.completeByAdmin(orderNo); adminAccountManagementApplicationService.recordOperation("ORDER", "ORDER_COMPLETE", "完结订单：" + orderNo, "SUCCESS"); return adminViewAssembler.toOrderDetail(orderFacade.getByOrderNo(orderNo)); }
    public AdminOrderOperationResultView handleException(String orderNo, AdminOrderExceptionHandleRequest request) {
        requireAdmin();
        if ("ADDRESS_ERROR".equalsIgnoreCase(request.exceptionType())) {
            orderFacade.reviseReceiverByAdmin(orderNo, request.receiverName(), request.receiverPhone(), request.receiverDetailAddress(), request.note());
        } else if ("PAYMENT_EXCEPTION".equalsIgnoreCase(request.exceptionType())) {
            orderFacade.markPaymentExceptionByAdmin(orderNo, request.note());
        } else {
            throw BusinessException.badRequest("暂不支持的异常处理类型");
        }
        adminAccountManagementApplicationService.recordOperation("ORDER", "ORDER_EXCEPTION_HANDLE", "处理订单异常：" + orderNo + "，类型=" + request.exceptionType(), "SUCCESS");
        return new AdminOrderOperationResultView(orderNo, request.exceptionType(), "SUCCESS", "处理完成");
    }
    public PayOrderView closePayOrder(String orderNo) { requireAdmin(); return payFacade.closePayOrder(orderNo, "ADMIN_CLOSED"); }

    private void requireAdmin() {
        AuthenticatedPrincipal principal = authFacade.currentPrincipal();
        if (principal == null || !principal.isAdmin()) throw BusinessException.forbidden("仅管理员可访问当前接口");
    }
}
