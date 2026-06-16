package com.mallfei.admin.application.service;

import com.mallfei.admin.application.assembler.AdminViewAssembler;
import com.mallfei.admin.application.dto.AdminOrderExceptionHandleRequest;
import com.mallfei.admin.application.dto.AdminOrderReceiverUpdateRequest;
import com.mallfei.admin.application.vo.AdminOrderDetailView;
import com.mallfei.admin.application.vo.AdminOrderOperationResultView;
import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.facade.OrderFacade;
import com.mallfei.pay.application.vo.PayOrderPaymentVerificationView;
import com.mallfei.pay.application.vo.PayOrderView;
import com.mallfei.pay.domain.model.PayReconciliationRecord;
import com.mallfei.pay.facade.PayFacade;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public AdminOrderDetailView updateReceiver(String orderNo, AdminOrderReceiverUpdateRequest request) {
        requireAdmin();
        Order latestOrder = orderFacade.reviseReceiverByAdmin(orderNo, request.receiverName(), request.receiverPhone(), request.receiverProvinceName(), request.receiverCityName(), request.receiverDistrictName(), request.receiverDetailAddress(), request.note());
        adminAccountManagementApplicationService.recordOperation("ORDER", "ORDER_RECEIVER_UPDATE", "修改订单收货地址：" + orderNo, "SUCCESS");
        return adminViewAssembler.toOrderDetail(latestOrder);
    }
    public AdminOrderOperationResultView handleException(String orderNo, AdminOrderExceptionHandleRequest request) {
        requireAdmin();
        OrderExceptionHandleStrategy strategy = OrderExceptionHandleStrategy.from(request.exceptionType());
        ensureActionAllowed(orderNo, strategy);
        Order handledOrder = switch (strategy) {
            case ADDRESS_ERROR -> orderFacade.reviseReceiverByAdmin(orderNo, request.receiverName(), request.receiverPhone(), request.receiverProvinceName(), request.receiverCityName(), request.receiverDistrictName(), request.receiverDetailAddress(), request.note());
            case PAYMENT_EXCEPTION -> {
                Order before = orderFacade.getByOrderNo(orderNo);
                int affectedRows = orderFacade.markPaymentExceptionByAdmin(orderNo, request.note());
                Order latest = orderFacade.getByOrderNo(orderNo);
                if (!Order.STATUS_PAYMENT_EXCEPTION.equals(latest.orderStatus())) {
                    throw BusinessException.badRequest("支付异常标记未生效：更新行数=" + affectedRows + "，原状态=" + before.orderStatus() + "，当前状态=" + latest.orderStatus() + "，请确认后端已重启并连接到当前页面使用的数据库");
                }
                yield latest;
            }
            case CONFIRM_PAID -> {
                orderFacade.confirmPaidByAdmin(orderNo, request.note());
                yield orderFacade.getByOrderNo(orderNo);
            }
            case RESTORE_PENDING_PAYMENT -> {
                orderFacade.restorePendingPaymentByAdmin(orderNo, request.note());
                yield orderFacade.getByOrderNo(orderNo);
            }
            case CLOSE_AND_RELEASE_STOCK -> {
                orderFacade.cancelByAdmin(orderNo);
                yield orderFacade.getByOrderNo(orderNo);
            }
            case USER_NEGOTIATION_RETURN -> orderFacade.createNegotiatedReturnRefundByAdmin(orderNo, request.note());
            case USER_NEGOTIATION_SWITCH_SKU -> orderFacade.switchSkuByNegotiationByAdmin(orderNo, request.orderItemId(), request.targetSkuId(), request.priceDifferenceHandleType(), request.note());
            case LOGISTICS_EXCEPTION -> orderFacade.returnToPaidForLogisticsExceptionByAdmin(orderNo, request.note());
        };
        AdminOrderDetailView latestOrder = adminViewAssembler.toOrderDetail(handledOrder);
        adminAccountManagementApplicationService.recordOperation("ORDER", operationCode(strategy), operationDescription(orderNo, strategy), "SUCCESS");
        return new AdminOrderOperationResultView(orderNo, strategy.name(), "SUCCESS", exceptionHandleMessage(strategy, latestOrder), latestOrder);
    }
    public PayOrderView closePayOrder(String orderNo) { requireAdmin(); return payFacade.closePayOrder(orderNo, "ADMIN_CLOSED"); }

    public PayOrderPaymentVerificationView verifyPaymentException(String orderNo) {
        requireAdmin();
        PayOrderPaymentVerificationView result = payFacade.verifyOrderPayment(orderNo);
        adminAccountManagementApplicationService.recordOperation("ORDER", "ORDER_PAYMENT_EXCEPTION_VERIFY", "核验订单支付异常：" + orderNo + "，结论=" + result.conclusion() + "，建议=" + result.suggestedAction(), "SUCCESS");
        return result;
    }

    public PayReconciliationRecord transferPaymentExceptionToPaySync(String orderNo, String note) {
        requireAdmin();
        ensurePaymentActionAllowed(orderNo, PayOrderPaymentVerificationView.ACTION_TRANSFER_PAY_SYNC);
        PayReconciliationRecord record = payFacade.transferPaySync(orderNo, note);
        adminAccountManagementApplicationService.recordOperation("ORDER", "ORDER_PAYMENT_EXCEPTION_TRANSFER_PAY_SYNC", "支付异常转支付同步状态：" + orderNo + "，对账记录ID=" + record.id(), "SUCCESS");
        return record;
    }

    public PayReconciliationRecord markPaymentExceptionPendingAction(String orderNo, String action, String note) {
        requireAdmin();
        String normalizedAction = action == null ? "" : action.trim().toUpperCase();
        if (!"ORDER_REPAIR_PENDING".equals(normalizedAction)
                && !"CLOSE_RELEASE_PENDING".equals(normalizedAction)
                && !"AMOUNT_RECONCILE_PENDING".equals(normalizedAction)
                && !"PAY_SYNC_PENDING".equals(normalizedAction)) {
            throw BusinessException.badRequest("不支持的支付单待处理标记");
        }
        ensurePaymentActionAllowed(orderNo, verificationActionOfPendingAction(normalizedAction));
        PayReconciliationRecord record = payFacade.markPaymentExceptionPendingAction(orderNo, normalizedAction, note);
        adminAccountManagementApplicationService.recordOperation("ORDER", "ORDER_PAYMENT_EXCEPTION_PENDING_ACTION", "支付异常标记待处理：" + orderNo + "，动作=" + normalizedAction + "，对账记录ID=" + record.id(), "SUCCESS");
        return record;
    }

    private void ensureActionAllowed(String orderNo, OrderExceptionHandleStrategy strategy) {
        if (strategy != OrderExceptionHandleStrategy.CONFIRM_PAID
                && strategy != OrderExceptionHandleStrategy.RESTORE_PENDING_PAYMENT
                && strategy != OrderExceptionHandleStrategy.CLOSE_AND_RELEASE_STOCK) {
            return;
        }
        ensurePaymentActionAllowed(orderNo, strategy.name());
    }

    private void ensurePaymentActionAllowed(String orderNo, String action) {
        PayOrderPaymentVerificationView verification = payFacade.verifyOrderPayment(orderNo);
        List<String> allowedActions = verification.allowedActions() == null ? List.of() : verification.allowedActions();
        if (!allowedActions.contains(action)) {
            throw BusinessException.badRequest("支付异常核验结论不允许执行当前动作：" + verification.message());
        }
    }

    private String verificationActionOfPendingAction(String pendingAction) {
        return switch (pendingAction) {
            case "PAY_SYNC_PENDING" -> PayOrderPaymentVerificationView.ACTION_TRANSFER_PAY_SYNC;
            case "ORDER_REPAIR_PENDING" -> PayOrderPaymentVerificationView.ACTION_CONFIRM_PAID;
            case "CLOSE_RELEASE_PENDING" -> PayOrderPaymentVerificationView.ACTION_CLOSE_AND_RELEASE_STOCK;
            case "AMOUNT_RECONCILE_PENDING" -> PayOrderPaymentVerificationView.ACTION_TRANSFER_AMOUNT_RECONCILE;
            default -> pendingAction;
        };
    }

    private String operationCode(OrderExceptionHandleStrategy strategy) {
        return switch (strategy) {
            case ADDRESS_ERROR -> "ORDER_RECEIVER_UPDATE";
            case PAYMENT_EXCEPTION -> "ORDER_PAYMENT_EXCEPTION_MARK";
            case CONFIRM_PAID -> "ORDER_PAYMENT_EXCEPTION_CONFIRM_PAID";
            case RESTORE_PENDING_PAYMENT -> "ORDER_PAYMENT_EXCEPTION_RESTORE_PENDING";
            case CLOSE_AND_RELEASE_STOCK -> "ORDER_PAYMENT_EXCEPTION_CLOSE_RELEASE";
            case USER_NEGOTIATION_RETURN -> "ORDER_NEGOTIATION_RETURN";
            case USER_NEGOTIATION_SWITCH_SKU -> "ORDER_NEGOTIATION_SWITCH_SKU";
            case LOGISTICS_EXCEPTION -> "ORDER_LOGISTICS_EXCEPTION_RETURN_PAID";
        };
    }

    private String operationDescription(String orderNo, OrderExceptionHandleStrategy strategy) {
        return switch (strategy) {
            case ADDRESS_ERROR -> "处理订单地址异常：" + orderNo;
            case PAYMENT_EXCEPTION -> "标记订单支付异常：" + orderNo;
            case CONFIRM_PAID -> "人工确认支付异常订单已支付：" + orderNo;
            case RESTORE_PENDING_PAYMENT -> "恢复支付异常订单为待支付：" + orderNo;
            case CLOSE_AND_RELEASE_STOCK -> "关闭支付异常订单并释放库存：" + orderNo;
            case USER_NEGOTIATION_RETURN -> "用户协商申请退货：" + orderNo;
            case USER_NEGOTIATION_SWITCH_SKU -> "用户协商切换SKU：" + orderNo;
            case LOGISTICS_EXCEPTION -> "物流异常模拟回退已支付：" + orderNo;
        };
    }

    private String exceptionHandleMessage(OrderExceptionHandleStrategy strategy, AdminOrderDetailView latestOrder) {
        return switch (strategy) {
            case ADDRESS_ERROR -> "处理完成，当前收货地址=" + latestOrder.address();
            case PAYMENT_EXCEPTION -> "已标记支付异常，订单暂停超时关单和发货履约";
            case CONFIRM_PAID -> "已人工确认支付，库存已确认扣减，可继续履约";
            case RESTORE_PENDING_PAYMENT -> "已恢复待支付，订单重新进入支付倒计时与超时关单流程";
            case CLOSE_AND_RELEASE_STOCK -> "已人工关闭订单并释放预占库存";
            case USER_NEGOTIATION_RETURN -> "已按用户协商创建退款申请并记录备注，订单保持当前履约状态，待售后审核/退款执行后再更新退款状态";
            case USER_NEGOTIATION_SWITCH_SKU -> "已完成同价SKU切换，原SKU库存已回补，新SKU库存已扣减";
            case LOGISTICS_EXCEPTION -> "已模拟物流异常，订单从已发货回退为已支付，发货时间保留用于追踪";
        };
    }

    private void requireAdmin() {
        AuthenticatedPrincipal principal = authFacade.currentPrincipal();
        if (principal == null || !principal.isAdmin()) throw BusinessException.forbidden("仅管理员可访问当前接口");
    }

    private enum OrderExceptionHandleStrategy {
        PAYMENT_EXCEPTION,
        ADDRESS_ERROR,
        USER_NEGOTIATION_RETURN,
        USER_NEGOTIATION_SWITCH_SKU,
        LOGISTICS_EXCEPTION,
        CONFIRM_PAID,
        RESTORE_PENDING_PAYMENT,
        CLOSE_AND_RELEASE_STOCK;

        static OrderExceptionHandleStrategy from(String value) {
            if (value == null || value.isBlank()) {
                throw BusinessException.badRequest("异常类型不能为空");
            }
            String normalizedValue = value.trim().toUpperCase();
            if ("USER_NEGOTIATION".equals(normalizedValue)) {
                throw BusinessException.badRequest("请选择用户协商的具体处理动作");
            }
            try {
                return OrderExceptionHandleStrategy.valueOf(normalizedValue);
            } catch (IllegalArgumentException exception) {
                throw BusinessException.badRequest("暂不支持的异常处理类型");
            }
        }
    }
}
