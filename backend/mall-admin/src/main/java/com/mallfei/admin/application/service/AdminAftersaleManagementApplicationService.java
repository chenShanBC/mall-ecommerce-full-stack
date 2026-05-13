package com.mallfei.admin.application.service;

import com.mallfei.admin.application.dto.AdminAftersaleReviewRequest;
import com.mallfei.admin.application.vo.AdminAftersaleDetailView;
import com.mallfei.aftersale.domain.model.AftersaleOrder;
import com.mallfei.aftersale.facade.AftersaleFacade;
import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class AdminAftersaleManagementApplicationService {

    private final AuthFacade authFacade;
    private final AftersaleFacade aftersaleFacade;
    private final AdminAccountManagementApplicationService adminAccountManagementApplicationService;

    public AdminAftersaleManagementApplicationService(AuthFacade authFacade,
                                                      AftersaleFacade aftersaleFacade,
                                                      AdminAccountManagementApplicationService adminAccountManagementApplicationService) {
        this.authFacade = authFacade;
        this.aftersaleFacade = aftersaleFacade;
        this.adminAccountManagementApplicationService = adminAccountManagementApplicationService;
    }

    public AdminAftersaleDetailView review(String aftersaleNo, AdminAftersaleReviewRequest request) {
        requireAdmin();
        AftersaleOrder aftersaleOrder;
        if ("APPROVE".equalsIgnoreCase(request.action())) {
            aftersaleOrder = aftersaleFacade.approve(aftersaleNo);
            adminAccountManagementApplicationService.recordOperation("AFTERSALE", "AFTERSALE_APPROVE", "审核通过售后单：" + aftersaleNo, "SUCCESS");
        } else if ("REJECT".equalsIgnoreCase(request.action())) {
            aftersaleOrder = aftersaleFacade.reject(aftersaleNo);
            adminAccountManagementApplicationService.recordOperation("AFTERSALE", "AFTERSALE_REJECT", "驳回售后单：" + aftersaleNo, "SUCCESS");
        } else {
            throw BusinessException.badRequest("暂不支持的审核动作");
        }
        return new AdminAftersaleDetailView(
                aftersaleOrder.id(),
                aftersaleOrder.aftersaleNo(),
                aftersaleOrder.orderNo(),
                aftersaleOrder.userId(),
                aftersaleOrder.aftersaleType(),
                aftersaleOrder.status(),
                aftersaleOrder.refundAmountCent(),
                aftersaleOrder.reason(),
                aftersaleOrder.createdAt(),
                aftersaleOrder.updatedAt()
        );
    }

    private void requireAdmin() {
        AuthenticatedPrincipal principal = authFacade.currentPrincipal();
        if (principal == null || !principal.isAdmin()) throw BusinessException.forbidden("仅管理员可访问当前接口");
    }
}
