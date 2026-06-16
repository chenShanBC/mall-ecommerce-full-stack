package com.mallfei.admin.application.service;

import com.mallfei.admin.application.dto.AdminAftersaleReviewRequest;
import com.mallfei.admin.application.vo.AdminAftersaleDetailView;
import com.mallfei.aftersale.domain.model.AftersaleOrder;
import com.mallfei.aftersale.facade.AftersaleFacade;
import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.application.dto.OrderRefundApplyRequest;
import com.mallfei.order.application.vo.OrderRefundView;
import com.mallfei.order.facade.OrderFacade;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminAftersaleManagementApplicationService {

    private final AuthFacade authFacade;
    private final OrderFacade orderFacade;
    private final AftersaleFacade aftersaleFacade;
    private final AdminAccountManagementApplicationService adminAccountManagementApplicationService;

    public AdminAftersaleManagementApplicationService(AuthFacade authFacade,
                                                      OrderFacade orderFacade,
                                                      AftersaleFacade aftersaleFacade,
                                                      AdminAccountManagementApplicationService adminAccountManagementApplicationService) {
        this.authFacade = authFacade;
        this.orderFacade = orderFacade;
        this.aftersaleFacade = aftersaleFacade;
        this.adminAccountManagementApplicationService = adminAccountManagementApplicationService;
    }

    public AdminAftersaleDetailView review(String aftersaleNo, AdminAftersaleReviewRequest request) {
        requireAdmin();
        if ("APPROVE".equalsIgnoreCase(request.action())) {
            return approve(aftersaleNo, request.reason());
        }
        if ("REJECT".equalsIgnoreCase(request.action())) {
            return reject(aftersaleNo, request.reason());
        }
        throw BusinessException.badRequest("暂不支持的审核动作");
    }

    private AdminAftersaleDetailView approve(String aftersaleNo, String reason) {
        AftersaleOrder approved = aftersaleFacade.approve(aftersaleNo);
        OrderRefundView refund = orderFacade.createRefundApplicationByAdmin(
                orderFacade.getByOrderNo(approved.orderNo()),
                new OrderRefundApplyRequest(blank(reason) ? approved.reason() : reason.trim(), null)
        );
        OrderRefundView refunding = orderFacade.approveRefundApplicationByAdmin(refund.refundNo());
        AftersaleOrder processing = aftersaleFacade.update(approved.bindRefundNo(refunding.refundNo(), LocalDateTime.now()));
        adminAccountManagementApplicationService.recordOperation("AFTERSALE", "AFTERSALE_APPROVE", "审核通过并生成退款单：" + aftersaleNo + "，退款单号：" + refund.refundNo(), "SUCCESS");
        return toAftersaleDetail(processing);
    }

    private AdminAftersaleDetailView reject(String aftersaleNo, String reason) {
        String safeReason = blank(reason) ? "商家审核驳回" : reason.trim();
        AftersaleOrder rejected = aftersaleFacade.reject(aftersaleNo, safeReason);
        orderFacade.rejectRefundToStatus(rejected.orderNo(), rejected.originOrderStatus(), safeReason);
        adminAccountManagementApplicationService.recordOperation("AFTERSALE", "AFTERSALE_REJECT", "审核驳回售后单：" + aftersaleNo + "，原因：" + safeReason, "SUCCESS");
        return toAftersaleDetail(rejected);
    }

    private AdminAftersaleDetailView toAftersaleDetail(AftersaleOrder order) {
        return new AdminAftersaleDetailView(order.id(), order.aftersaleNo(), order.orderNo(), order.userId(), order.aftersaleType(), order.status(), order.refundAmountCent(), order.reason(), order.rejectReason(), order.failReason(), order.createdAt(), order.updatedAt());
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void requireAdmin() {
        AuthenticatedPrincipal principal = authFacade.currentPrincipal();
        if (principal == null || !principal.isAdmin()) throw BusinessException.forbidden("仅管理员可访问当前接口");
    }
}
