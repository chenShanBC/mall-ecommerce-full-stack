package com.mallfei.aftersale.application.service;

import com.mallfei.aftersale.application.dto.AftersaleRefundApplyRequest;
import com.mallfei.aftersale.application.vo.AftersaleRefundApplyView;
import com.mallfei.aftersale.domain.model.AftersaleOrder;
import com.mallfei.aftersale.domain.repository.AftersaleOrderRepository;
import com.mallfei.aftersale.domain.service.AftersaleDomainService;
import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.application.dto.OrderRefundFailedEvent;
import com.mallfei.order.application.dto.OrderRefundSucceededEvent;
import com.mallfei.order.config.OrderMqConfig;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.facade.OrderFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AftersaleApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AftersaleApplicationService.class);

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
                order.orderStatus(),
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

    public AftersaleOrder createOnlyRefund(String orderNo, Long userId, String originOrderStatus, Long refundAmountCent, String reason) {
        return aftersaleDomainService.save(AftersaleOrder.createOnlyRefund(newAftersaleNo(), orderNo, userId, originOrderStatus, refundAmountCent, reason, LocalDateTime.now()));
    }

    public java.util.List<AftersaleOrder> currentUserAftersales(String orderNo) {
        AuthenticatedPrincipal principal = currentUser();
        return aftersaleOrderRepository.findAll().stream()
                .filter(item -> principal.principalId().equals(item.userId()))
                .filter(item -> orderNo == null || orderNo.isBlank() || orderNo.equals(item.orderNo()))
                .toList();
    }

    @RabbitListener(queues = OrderMqConfig.AFTERSALE_REFUND_SUCCEEDED_QUEUE)
    public void onRefundSucceeded(OrderRefundSucceededEvent event) {
        aftersaleOrderRepository.findLatestByOrderNo(event.orderNo())
                .filter(aftersaleOrder -> refundNoMatches(aftersaleOrder, event.refundNo()))
                .ifPresentOrElse(aftersaleOrder -> {
                    if (AftersaleOrder.STATUS_REFUND_SUCCESS.equals(aftersaleOrder.status())) {
                        log.info("Ignoring duplicated aftersale refund succeeded event, orderNo={}, refundNo={}", event.orderNo(), event.refundNo());
                        return;
                    }
                    if (!AftersaleOrder.STATUS_REFUND_PROCESSING.equals(aftersaleOrder.status())) {
                        log.warn("Skipped aftersale refund success transition because status is not processing, aftersaleNo={}, orderNo={}, refundNo={}, status={}",
                                aftersaleOrder.aftersaleNo(), event.orderNo(), event.refundNo(), aftersaleOrder.status());
                        return;
                    }
                    aftersaleDomainService.update(aftersaleOrder.markRefundSuccess(LocalDateTime.now()));
                    log.info("Marked aftersale refund success from pay event, aftersaleNo={}, orderNo={}, refundNo={}, channelRefundNo={}",
                            aftersaleOrder.aftersaleNo(), event.orderNo(), event.refundNo(), event.channelRefundNo());
                }, () -> log.debug("No aftersale order matched refund success event, orderNo={}, refundNo={}", event.orderNo(), event.refundNo()));
    }

    @RabbitListener(queues = OrderMqConfig.AFTERSALE_REFUND_FAILED_QUEUE)
    public void onRefundFailed(OrderRefundFailedEvent event) {
        aftersaleOrderRepository.findLatestByOrderNo(event.orderNo())
                .filter(aftersaleOrder -> refundNoMatches(aftersaleOrder, event.refundNo()))
                .ifPresentOrElse(aftersaleOrder -> {
                    if (AftersaleOrder.STATUS_REFUND_SUCCESS.equals(aftersaleOrder.status())) {
                        log.warn("Ignoring aftersale refund failed event because aftersale already succeeded, aftersaleNo={}, orderNo={}, refundNo={}",
                                aftersaleOrder.aftersaleNo(), event.orderNo(), event.refundNo());
                        return;
                    }
                    if (AftersaleOrder.STATUS_REFUND_FAILED.equals(aftersaleOrder.status())) {
                        log.info("Ignoring duplicated aftersale refund failed event, orderNo={}, refundNo={}", event.orderNo(), event.refundNo());
                        return;
                    }
                    if (!AftersaleOrder.STATUS_REFUND_PROCESSING.equals(aftersaleOrder.status())) {
                        log.warn("Skipped aftersale refund failed transition because status is not processing, aftersaleNo={}, orderNo={}, refundNo={}, status={}",
                                aftersaleOrder.aftersaleNo(), event.orderNo(), event.refundNo(), aftersaleOrder.status());
                        return;
                    }
                    aftersaleDomainService.update(aftersaleOrder.markRefundFailed(event.failReason(), LocalDateTime.now()));
                    log.warn("Marked aftersale refund failed from pay event, aftersaleNo={}, orderNo={}, refundNo={}, reason={}",
                            aftersaleOrder.aftersaleNo(), event.orderNo(), event.refundNo(), event.failReason());
                }, () -> log.debug("No aftersale order matched refund failed event, orderNo={}, refundNo={}", event.orderNo(), event.refundNo()));
    }

    private boolean refundNoMatches(AftersaleOrder aftersaleOrder, String refundNo) {
        return aftersaleOrder.refundNo() != null && aftersaleOrder.refundNo().equals(refundNo);
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
