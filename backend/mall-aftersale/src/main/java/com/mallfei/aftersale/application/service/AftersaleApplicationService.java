package com.mallfei.aftersale.application.service;

import com.mallfei.aftersale.application.dto.AftersaleRefundApplyRequest;
import com.mallfei.aftersale.application.vo.AftersaleRefundApplyView;
import com.mallfei.aftersale.domain.model.AftersaleOrder;
import com.mallfei.aftersale.domain.repository.AftersaleOrderRepository;
import com.mallfei.aftersale.domain.service.AftersaleDomainService;
import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.facade.OrderFacade;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AftersaleApplicationService {

    private final AftersaleDomainService aftersaleDomainService;
    private final AftersaleOrderRepository aftersaleOrderRepository;
    private final AuthFacade authFacade;
    private final OrderFacade orderFacade;

    public AftersaleApplicationService(AftersaleDomainService aftersaleDomainService,
                                       AftersaleOrderRepository aftersaleOrderRepository,
                                       AuthFacade authFacade,
                                       OrderFacade orderFacade) {
        this.aftersaleDomainService = aftersaleDomainService;
        this.aftersaleOrderRepository = aftersaleOrderRepository;
        this.authFacade = authFacade;
        this.orderFacade = orderFacade;
    }

    public AftersaleRefundApplyView applyOnlyRefund(AftersaleRefundApplyRequest request) {
        AuthenticatedPrincipal principal = currentUser();
        Order order = orderFacade.getOwnedOrder(request.orderId(), principal.principalId());
        order.ensureRefundable();
        if (aftersaleOrderRepository.findLatestByOrderNo(order.orderNo()).isPresent()) {
            throw BusinessException.badRequest("该订单已提交售后申请，请勿重复提交");
        }
        orderFacade.markRefundPending(order.orderNo());
        AftersaleOrder aftersaleOrder = aftersaleDomainService.save(AftersaleOrder.createOnlyRefund(
                newAftersaleNo(),
                order.orderNo(),
                principal.principalId(),
                order.safePayAmountCent(),
                request.reason().trim(),
                LocalDateTime.now()
        ));
        return new AftersaleRefundApplyView(
                aftersaleOrder.id(),
                aftersaleOrder.aftersaleNo(),
                aftersaleOrder.orderNo(),
                aftersaleOrder.status(),
                aftersaleOrder.reason(),
                aftersaleOrder.createdAt()
        );
    }

    public AftersaleOrder createOnlyRefund(String orderNo, Long userId, Long refundAmountCent, String reason) {
        return aftersaleDomainService.save(AftersaleOrder.createOnlyRefund(newAftersaleNo(), orderNo, userId, refundAmountCent, reason, LocalDateTime.now()));
    }

    private AuthenticatedPrincipal currentUser() {
        AuthenticatedPrincipal principal = authFacade.currentPrincipal();
        if (principal == null || !principal.isUser()) {
            throw BusinessException.forbidden("仅用户可访问当前接口");
        }
        return principal;
    }

    private String newAftersaleNo() {
        return "AFT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
