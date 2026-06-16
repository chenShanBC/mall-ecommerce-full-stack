package com.mallfei.pay.application.service;

import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.facade.OrderFacade;
import com.mallfei.pay.application.assembler.PayViewAssembler;
import com.mallfei.pay.application.vo.PayOrderPaymentVerificationView;
import com.mallfei.pay.application.vo.PayOrderView;
import com.mallfei.pay.application.vo.PayReconcileResultView;
import com.mallfei.pay.config.AlipaySandboxProperties;
import com.mallfei.pay.domain.model.PayCallbackRecord;
import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.pay.domain.model.PayReconciliationRecord;
import com.mallfei.pay.domain.model.PayRefundOrder;
import com.mallfei.pay.domain.repository.PayCallbackRecordRepository;
import com.mallfei.pay.domain.repository.PayOrderRepository;
import com.mallfei.pay.domain.repository.PayReconciliationRecordRepository;
import com.mallfei.pay.domain.repository.PayRefundOrderRepository;
import com.mallfei.pay.domain.service.PayChannelCallbackRequest;
import com.mallfei.pay.domain.service.PayChannelClientRouter;
import com.mallfei.pay.domain.service.PayChannelQueryResult;
import com.mallfei.pay.domain.service.PayChannelSubmitResult;
import com.mallfei.pay.domain.service.PayOrderDomainService;
import com.mallfei.pay.domain.service.PayRefundQueryRequest;
import com.mallfei.pay.domain.service.PayRefundQueryResult;
import com.mallfei.pay.domain.service.PayRefundRequest;
import com.mallfei.pay.domain.service.PayRefundResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PayApplicationService {

    private static final Logger log = LoggerFactory.getLogger(PayApplicationService.class);

    private final PayOrderRepository payOrderRepository;
    private final PayCallbackRecordRepository payCallbackRecordRepository;
    private final PayRefundOrderRepository payRefundOrderRepository;
    private final PayReconciliationRecordRepository payReconciliationRecordRepository;
    private final PayOrderDomainService payOrderDomainService;
    private final OrderFacade orderFacade;
    private final PayViewAssembler payViewAssembler;
    private final PayChannelClientRouter payChannelClientRouter;
    private final PayRefundResultPublisher payRefundResultPublisher;
    private final AlipaySandboxProperties alipaySandboxProperties;

    public PayApplicationService(PayOrderRepository payOrderRepository,
                                 PayCallbackRecordRepository payCallbackRecordRepository,
                                 PayRefundOrderRepository payRefundOrderRepository,
                                 PayReconciliationRecordRepository payReconciliationRecordRepository,
                                 PayOrderDomainService payOrderDomainService,
                                 OrderFacade orderFacade,
                                 PayViewAssembler payViewAssembler,
                                 PayChannelClientRouter payChannelClientRouter,
                                 PayRefundResultPublisher payRefundResultPublisher,
                                 AlipaySandboxProperties alipaySandboxProperties) {
        this.payOrderRepository = payOrderRepository;
        this.payCallbackRecordRepository = payCallbackRecordRepository;
        this.payRefundOrderRepository = payRefundOrderRepository;
        this.payReconciliationRecordRepository = payReconciliationRecordRepository;
        this.payOrderDomainService = payOrderDomainService;
        this.orderFacade = orderFacade;
        this.payViewAssembler = payViewAssembler;
        this.payChannelClientRouter = payChannelClientRouter;
        this.payRefundResultPublisher = payRefundResultPublisher;
        this.alipaySandboxProperties = alipaySandboxProperties;
    }

    public PayOrderView createPayOrder(String orderNo) {
        return createPayOrder(orderNo, PayOrder.CHANNEL_MOCK);
    }

    public PayOrderView createPayOrder(String orderNo, String payChannel) {
        return createPayOrder(orderNo, payChannel, "/orders");
    }

    public PayOrderView createPayOrder(String orderNo, String payChannel, String returnPath) {
        orderFacade.closeIfTimedOut(orderNo);
        Order order = orderFacade.getByOrderNo(orderNo);
        ensureOrderPayable(order);
        Duration remainingPayTime = remainingPayTime(order);
        String resolvedChannel = resolvePayChannel(payChannel);
        PayOrder existing = findReusablePayOrder(orderNo, resolvedChannel);
        if (existing != null) {
            payOrderDomainService.ensureExistingPayOrderUsable(existing, order);
            log.info("Reusing existing pay order, orderNo={}, payOrderNo={}, payChannel={}, payStatus={}",
                    orderNo, existing.payOrderNo(), existing.payChannel(), existing.payStatus());
            PayChannelSubmitResult submitResult = buildSubmitResult(existing, returnPath, remainingPayTime);
            log.info("Generated pay submit payload for reused order, orderNo={}, payOrderNo={}, payChannel={}, hasRedirectForm={}, redirectUrl={}",
                    orderNo, existing.payOrderNo(), existing.payChannel(), submitResult.redirectForm() != null && !submitResult.redirectForm().isBlank(), submitResult.redirectUrl());
            PayOrder resubmittedOrder = existing.withSubmission(submitResult.displayPayload());
            payOrderDomainService.update(resubmittedOrder);
            return payViewAssembler.toView(payOrderDomainService.loadByPayOrderNo(existing.payOrderNo()), submitResult);
        }
        closeOtherPendingPayOrders(orderNo, resolvedChannel);
        PayOrder payOrder = payOrderDomainService.save(payOrderDomainService.createPending(newPayOrderNo(), order, resolvedChannel));
        PayChannelSubmitResult submitResult = buildSubmitResult(payOrder, returnPath, remainingPayTime);
        log.info("Generated pay submit payload for new order, orderNo={}, payOrderNo={}, payChannel={}, hasRedirectForm={}, redirectUrl={}",
                orderNo, payOrder.payOrderNo(), payOrder.payChannel(), submitResult.redirectForm() != null && !submitResult.redirectForm().isBlank(), submitResult.redirectUrl());
        PayOrder submittedOrder = payOrder.withSubmission(submitResult.displayPayload());
        payOrderDomainService.update(submittedOrder);
        log.info("Created pay order, orderNo={}, payOrderNo={}, payChannel={}", orderNo, payOrder.payOrderNo(), payOrder.payChannel());
        return payViewAssembler.toView(payOrderDomainService.loadByPayOrderNo(payOrder.payOrderNo()), submitResult);
    }

    public PayOrderView markMockSuccess(String orderNo) {
        Order order = orderFacade.getByOrderNo(orderNo);
        PayOrder payOrder = requireLatestPayOrder(orderNo);
        payOrderDomainService.ensureCanCallbackSuccess(payOrder, order);
        String transactionNo = payOrder.transactionNo() == null || payOrder.transactionNo().isBlank()
                ? "MOCK-" + orderNo
                : payOrder.transactionNo();
        String rawPayload = "{\"channel\":\"MOCK\",\"orderNo\":\"" + orderNo
                + "\",\"payOrderNo\":\"" + payOrder.payOrderNo()
                + "\",\"transactionNo\":\"" + transactionNo
                + "\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":" + payOrder.payAmountCent() + "}";
        PayChannelCallbackRequest request = new PayChannelCallbackRequest(
                PayOrder.CHANNEL_MOCK,
                payOrder.payOrderNo(),
                orderNo,
                payOrder.payOrderNo(),
                transactionNo,
                "MOCK-SIGN",
                rawPayload,
                "TRADE_SUCCESS",
                payOrder.payAmountCent()
        );
        return handleChannelCallback(request);
    }

    public boolean handleAlipayCallback(Map<String, String> params) {
        String payOrderNo = params.get("out_trade_no");
        PayOrder callbackPayOrder = payOrderNo == null || payOrderNo.isBlank() ? null : payOrderRepository.findByPayOrderNo(payOrderNo).orElse(null);
        String callbackChannel = callbackPayOrder == null ? PayOrder.CHANNEL_ALIPAY_WAP : callbackPayOrder.payChannel();
        PayChannelCallbackRequest request = new PayChannelCallbackRequest(
                callbackChannel,
                payOrderNo,
                params.get("passback_params"),
                params.get("out_trade_no"),
                params.get("trade_no"),
                params.get("sign"),
                buildAlipayRawPayload(params),
                params.get("trade_status"),
                parseAlipayAmountCent(params.get("total_amount"))
        );
        log.info("Received Alipay callback, orderNo={}, payOrderNo={}, tradeNo={}, tradeStatus={}, paramKeys={}",
                request.orderNo(), request.payOrderNo(), request.transactionNo(), request.tradeStatus(), params.keySet());
        try {
            PayOrderView result = handleChannelCallback(request);
            boolean success = result != null && ("TRADE_SUCCESS".equalsIgnoreCase(request.tradeStatus()) || "TRADE_FINISHED".equalsIgnoreCase(request.tradeStatus()));
            log.info("Finished Alipay callback, orderNo={}, payOrderNo={}, tradeNo={}, tradeStatus={}, callbackAccepted={}",
                    request.orderNo(), request.payOrderNo(), request.transactionNo(), request.tradeStatus(), success);
            return success;
        } catch (Exception exception) {
            log.error("Failed to handle Alipay callback, orderNo={}, payOrderNo={}, tradeNo={}, tradeStatus={}",
                    request.orderNo(), request.payOrderNo(), request.transactionNo(), request.tradeStatus(), exception);
            return false;
        }
    }

    public PayOrderView handleChannelCallback(PayChannelCallbackRequest request) {
        boolean verified = payChannelClientRouter.route(request.channelCode()).verifyCallback(request);
        log.info("Verifying pay callback, channelCode={}, orderNo={}, payOrderNo={}, tradeNo={}, verified={}, tradeStatus={}",
                request.channelCode(), request.orderNo(), request.payOrderNo(), request.transactionNo(), verified, request.tradeStatus());
        PayCallbackRecord callbackRecord = payCallbackRecordRepository.save(PayCallbackRecord.createPay(
                request.channelCode(),
                request.payOrderNo(),
                request.orderNo(),
                request.outTradeNo(),
                request.transactionNo(),
                request.amountCent(),
                request.tradeStatus(),
                request.signature(),
                verified,
                request.rawPayload(),
                LocalDateTime.now()
        ));
        PayOrder payOrder = locatePayOrderForCallback(request.orderNo(), request.payOrderNo());
        String orderNo = request.orderNo() == null || request.orderNo().isBlank()
                ? payOrder.orderNo()
                : request.orderNo();
        if (!verified) {
            log.warn("Rejected pay callback because signature verification failed, channelCode={}, orderNo={}, payOrderNo={}, tradeNo={}",
                    request.channelCode(), orderNo, request.payOrderNo(), request.transactionNo());
            payCallbackRecordRepository.update(callbackRecord.markVerifyFailed("支付宝回调验签失败"));
            return payViewAssembler.toView(payOrder);
        }
        if (!payOrder.payOrderNo().equals(request.outTradeNo()) || !payOrder.payOrderNo().equals(request.payOrderNo()) || !payOrder.orderNo().equals(orderNo)) {
            log.warn("Rejected pay callback because business identity mismatched, requestOrderNo={}, localOrderNo={}, requestPayOrderNo={}, outTradeNo={}, localPayOrderNo={}",
                    orderNo, payOrder.orderNo(), request.payOrderNo(), request.outTradeNo(), payOrder.payOrderNo());
            payCallbackRecordRepository.update(callbackRecord.markBusinessMismatch("支付单号或订单号不匹配"));
            return payViewAssembler.toView(payOrder);
        }
        if (request.amountCent() != null && !payOrder.amountConsistentWith(request.amountCent())) {
            log.warn("Rejected pay callback because amount mismatched, orderNo={}, payOrderNo={}, callbackAmountCent={}, localAmountCent={}",
                    orderNo, payOrder.payOrderNo(), request.amountCent(), payOrder.payAmountCent());
            payCallbackRecordRepository.update(callbackRecord.markAmountMismatch("支付金额不一致"));
            return payViewAssembler.toView(payOrder);
        }
        if (payOrder.success()) {
            log.info("Ignoring duplicated successful pay callback, orderNo={}, payOrderNo={}, tradeNo={}",
                    orderNo, payOrder.payOrderNo(), request.transactionNo());
            payCallbackRecordRepository.update(callbackRecord.markIgnoredDuplicate("支付单已成功"));
            return payViewAssembler.toView(payOrder);
        }
        if (request.tradeStatus() != null && !request.tradeStatus().isBlank()
                && !"TRADE_SUCCESS".equalsIgnoreCase(request.tradeStatus())
                && !"TRADE_FINISHED".equalsIgnoreCase(request.tradeStatus())) {
            log.info("Ignoring pay callback because trade status is not success, orderNo={}, payOrderNo={}, tradeNo={}, tradeStatus={}",
                    orderNo, payOrder.payOrderNo(), request.transactionNo(), request.tradeStatus());
            payCallbackRecordRepository.update(callbackRecord.markIgnoredNonSuccess("非支付成功交易状态"));
            return payViewAssembler.toView(payOrder);
        }
        orderFacade.closeIfTimedOut(orderNo);
        Order order = orderFacade.getByOrderNo(orderNo);
        if (payOrder.shouldEscalateCallbackFor(order)) {
            PayOrder successOrder = payOrder.markSuccess(request.transactionNo() == null ? request.channelCode() + "-TX" : request.transactionNo(), LocalDateTime.now());
            payOrderDomainService.update(successOrder);
            payCallbackRecordRepository.update(callbackRecord.markProcessed());
            log.warn("Recorded successful pay callback for payment exception order; waiting for admin confirmation, orderNo={}, payOrderNo={}, tradeNo={}, orderStatus={}",
                    orderNo, successOrder.payOrderNo(), successOrder.transactionNo(), order.orderStatus());
            return payViewAssembler.toView(payOrderDomainService.loadByPayOrderNo(successOrder.payOrderNo()));
        }
        payOrderDomainService.ensureCanCallbackSuccess(payOrder, order);
        orderFacade.markPaid(orderNo);
        PayOrder successOrder = payOrder.markSuccess(request.transactionNo() == null ? request.channelCode() + "-TX" : request.transactionNo(), LocalDateTime.now());
        payOrderDomainService.update(successOrder);
        payCallbackRecordRepository.update(callbackRecord.markProcessed());
        log.info("Processed pay callback successfully, orderNo={}, payOrderNo={}, tradeNo={}, orderStatus={}, payStatus={}",
                orderNo, successOrder.payOrderNo(), successOrder.transactionNo(), orderFacade.getByOrderNo(orderNo).orderStatus(), successOrder.payStatus());
        return payViewAssembler.toView(payOrderDomainService.loadByPayOrderNo(successOrder.payOrderNo()));
    }

    public PayReconcileResultView reconcile(String orderNo) {
        String batchNo = newReconcileBatchNo(PayReconciliationRecord.BIZ_TYPE_PAY);
        Order order = orderFacade.getByOrderNo(orderNo);
        PayOrder payOrder = payOrderRepository.findByOrderNo(orderNo).orElse(null);
        PayChannelQueryResult channelResult = queryChannelForPayReconciliation(payOrder);
        PayOrder reconciledPayOrder = compensatePayOrderFromChannel(order, payOrder, channelResult);
        Order reconciledOrder = compensateOrderFromPayOrder(order, reconciledPayOrder, channelResult);
        boolean amountConsistent = reconciledPayOrder == null || payOrderDomainService.reconcileAmount(reconciledOrder, reconciledPayOrder);
        boolean statusConsistent = payStatusConsistent(reconciledOrder, reconciledPayOrder, channelResult);
        boolean consistent = amountConsistent && statusConsistent;
        recordPayReconciliation(batchNo, reconciledOrder, reconciledPayOrder, channelResult, amountConsistent, statusConsistent, consistent);
        return payViewAssembler.toReconcileResult(orderNo, reconciledOrder, reconciledPayOrder, consistent);
    }

    public PayOrderPaymentVerificationView verifyOrderPayment(String orderNo) {
        Order order = orderFacade.getByOrderNo(orderNo);
        PayOrder payOrder = payOrderRepository.findByOrderNo(orderNo).orElse(null);
        PayCallbackRecord callbackRecord = latestPayCallback(payOrder, orderNo);
        PayChannelQueryResult channelResult = queryChannelForVerification(payOrder);
        boolean payOrderExists = payOrder != null;
        boolean localPaid = payOrder != null && payOrder.success();
        Boolean channelPaid = channelResult == null ? null : channelResult.paid();
        boolean channelKnown = channelResult != null && !"CHANNEL_QUERY_FAILED".equalsIgnoreCase(channelResult.tradeStatus());
        boolean amountConsistent = payOrder == null || payOrder.amountConsistentWith(order.payAmountCent());
        VerificationDecision decision = decidePaymentExceptionAction(order, payOrder, callbackRecord, channelResult, localPaid, channelKnown, channelPaid, amountConsistent);
        return new PayOrderPaymentVerificationView(
                order.orderNo(),
                order.orderStatus(),
                order.payAmountCent(),
                payOrder == null ? null : payOrder.payOrderNo(),
                payOrder == null ? "NO_PAY_ORDER" : payOrder.payStatus(),
                payOrder == null ? null : payOrder.payChannel(),
                payOrder == null ? null : payOrder.transactionNo(),
                payOrder == null ? null : payOrder.payAmountCent(),
                channelResult == null ? "NO_PAY_ORDER" : channelResult.tradeStatus(),
                channelKnown,
                channelPaid,
                callbackRecord == null ? "NO_CALLBACK" : callbackRecord.processStatus(),
                callbackRecord == null ? null : callbackRecord.tradeStatus(),
                callbackRecord != null && callbackRecord.verified(),
                payOrderExists,
                amountConsistent,
                decision.conclusion(),
                decision.riskLevel(),
                decision.suggestedAction(),
                decision.allowedActions(),
                decision.message(),
                LocalDateTime.now()
        );
    }

    public PayReconciliationRecord transferPaySync(String orderNo, String remark) {
        Order order = orderFacade.getByOrderNo(orderNo);
        PayOrder payOrder = requireLatestPayOrder(orderNo);
        PayChannelQueryResult channelResult = queryChannelForVerification(payOrder);
        if (payOrder.success()) {
            throw BusinessException.badRequest("当前支付单已成功，无需转支付同步处理");
        }
        if (channelResult == null || !channelResult.paid()) {
            throw BusinessException.badRequest("仅当渠道已支付但本地支付单未成功时，才允许转支付同步处理");
        }
        if (!payOrder.amountConsistentWith(order.payAmountCent())) {
            throw BusinessException.badRequest("订单金额与支付单金额不一致，请转对账处理");
        }
        PayReconciliationRecord record = PayReconciliationRecord.paySyncPending(new PayReconciliationRecord.OrderPaySyncPendingSnapshot(
                orderNo,
                payOrder.payOrderNo(),
                order.orderStatus() + "/" + payOrder.payStatus(),
                channelResult.tradeStatus(),
                order.payAmountCent(),
                payOrder.payAmountCent(),
                remark == null || remark.isBlank() ? "订单支付异常核验发现本地支付状态与渠道状态不一致，待支付管理同步" : remark.trim()
        ));
        PayReconciliationRecord saved = payReconciliationRecordRepository.save(record);
        log.warn("Transferred payment exception to pay sync pending, orderNo={}, payOrderNo={}, localStatus={}, channelStatus={}",
                orderNo, payOrder.payOrderNo(), payOrder.payStatus(), channelResult.tradeStatus());
        return saved;
    }

    public PayReconciliationRecord markPaymentExceptionPendingAction(String orderNo, String action, String remark) {
        Order order = orderFacade.getByOrderNo(orderNo);
        PayOrder payOrder = requireLatestPayOrder(orderNo);
        String normalizedAction = action == null ? "" : action.trim().toUpperCase();
        PayReconciliationRecord.OrderRepairPendingSnapshot snapshot = new PayReconciliationRecord.OrderRepairPendingSnapshot(
                orderNo,
                payOrder.payOrderNo(),
                order.orderStatus() + "/" + payOrder.payStatus(),
                null,
                order.payAmountCent(),
                payOrder.payAmountCent(),
                remark == null || remark.isBlank() ? defaultPendingActionRemark(normalizedAction) : remark.trim()
        );
        PayReconciliationRecord record = switch (normalizedAction) {
            case "ORDER_REPAIR_PENDING" -> PayReconciliationRecord.orderRepairPending(snapshot);
            case "CLOSE_RELEASE_PENDING" -> PayReconciliationRecord.closeReleasePending(snapshot);
            case "AMOUNT_RECONCILE_PENDING" -> PayReconciliationRecord.pay(new PayReconciliationRecord.OrderPayReconciliationSnapshot(
                    snapshot.orderNo(), snapshot.payOrderNo(), snapshot.localStatus(), snapshot.channelStatus(), snapshot.localAmountCent(), snapshot.channelAmountCent(), true, true, false, snapshot.remark()));
            case "PAY_SYNC_PENDING" -> PayReconciliationRecord.paySyncPending(new PayReconciliationRecord.OrderPaySyncPendingSnapshot(
                    snapshot.orderNo(), snapshot.payOrderNo(), snapshot.localStatus(), snapshot.channelStatus(), snapshot.localAmountCent(), snapshot.channelAmountCent(), snapshot.remark()));
            default -> throw BusinessException.badRequest("不支持的支付单待处理标记");
        };
        PayReconciliationRecord saved = payReconciliationRecordRepository.save(record);
        log.warn("Marked payment exception pending action, orderNo={}, payOrderNo={}, action={}", orderNo, payOrder.payOrderNo(), normalizedAction);
        return saved;
    }

    private String defaultPendingActionRemark(String action) {
        return switch (action) {
            case "ORDER_REPAIR_PENDING" -> "订单支付异常已标记：待支付单管理补偿订单支付状态";
            case "CLOSE_RELEASE_PENDING" -> "订单支付异常已标记：待支付单管理关闭订单并释放库存";
            case "AMOUNT_RECONCILE_PENDING" -> "订单支付异常已标记：待对账管理核对金额差异";
            case "PAY_SYNC_PENDING" -> "订单支付异常已标记：待支付单管理同步支付状态";
            default -> "订单支付异常已标记待处理";
        };
    }

    public PayOrderView repairPaidOrder(String orderNo) {
        Order order = orderFacade.getByOrderNo(orderNo);
        PayOrder payOrder = requireLatestPayOrder(orderNo);
        if (payOrder.refunded()) {
            throw BusinessException.badRequest("已退款或部分退款的支付单无需补偿订单支付状态");
        }
        if (PayOrder.STATUS_REFUND_PENDING.equals(payOrder.payStatus())
                || PayOrder.STATUS_REFUNDING.equals(payOrder.payStatus())
                || PayOrder.STATUS_REFUND_FAILED.equals(payOrder.payStatus())) {
            throw BusinessException.badRequest("退款流程中的支付单无需补偿订单支付状态，请在退款单中处理退款进度");
        }
        if (order.paidOrAfter() && payOrder.success()) {
            return payViewAssembler.toView(payOrder);
        }
        if (!payOrder.success()) {
            throw BusinessException.badRequest("当前支付单尚未成功，无法补偿订单支付状态");
        }
        if (!order.pendingPayment() && !order.paymentException()) {
            throw BusinessException.badRequest("当前订单状态不允许补偿为已支付");
        }
        orderFacade.repairPaidByAdmin(orderNo, "PAY_REPAIR: 支付单已成功，后台补偿订单支付状态");
        markPendingReconciliationDone(orderNo, PayReconciliationRecord.DIFF_PAY_SUCCESS_ORDER_NOT_PAID, "支付单补偿订单成功，自动关闭待处理标记");
        log.warn("Repaired paid order from successful pay order, orderNo={}, payOrderNo={}, payChannel={}",
                orderNo, payOrder.payOrderNo(), payOrder.payChannel());
        return payViewAssembler.toView(payOrder);
    }

    public PayOrderView syncOrderStatus(String orderNo) {
        Order order = orderFacade.getByOrderNo(orderNo);
        PayOrder payOrder = requireLatestPayOrder(orderNo);
        PayChannelQueryResult queryResult = null;
        if (!payOrder.success() && isSyncQueryableChannel(payOrder.payChannel())) {
            queryResult = payChannelClientRouter.route(payOrder.payChannel()).query(payOrder);
            if (queryResult.paid()) {
                payOrder = payOrder.markSuccess(queryResult.transactionNo(), LocalDateTime.now());
                payOrderDomainService.update(payOrder);
                log.warn("Marked pay order success from channel query, orderNo={}, payOrderNo={}, payChannel={}, tradeNo={}, tradeStatus={}",
                        orderNo, payOrder.payOrderNo(), payOrder.payChannel(), queryResult.transactionNo(), queryResult.tradeStatus());
            }
        }
        if (payOrder.success() && (order.pendingPayment() || order.paymentException())) {
            orderFacade.repairPaidByAdmin(orderNo, "PAY_SYNC: 支付渠道已成功，后台同步支付状态自动补偿订单");
            markPendingReconciliationDone(orderNo, PayReconciliationRecord.DIFF_LOCAL_PAYING_CHANNEL_SUCCESS, "支付状态同步并补偿订单成功，自动关闭待处理标记");
            markPendingReconciliationDone(orderNo, PayReconciliationRecord.DIFF_PAY_SUCCESS_ORDER_NOT_PAID, "支付状态同步并补偿订单成功，自动关闭待处理标记");
            log.warn("Synced order status from successful pay order, orderNo={}, payOrderNo={}, payChannel={}",
                    orderNo, payOrder.payOrderNo(), payOrder.payChannel());
        } else if (payOrder.success()) {
            markPendingReconciliationDone(orderNo, PayReconciliationRecord.DIFF_LOCAL_PAYING_CHANNEL_SUCCESS, "支付状态已成功，自动关闭同步待处理标记");
        } else if (queryResult != null) {
            markPendingReconciliationDone(orderNo, PayReconciliationRecord.DIFF_LOCAL_PAYING_CHANNEL_SUCCESS, "已查询渠道状态，当前未确认支付成功，无需继续保留支付同步待处理标记");
        }
        return payViewAssembler.toView(payOrderDomainService.loadByPayOrderNo(payOrder.payOrderNo()));
    }

    private void markPendingReconciliationDone(String orderNo, String diffType, String remark) {
        payReconciliationRecordRepository.findPendingByOrderNoAndDiffType(orderNo, diffType, 100)
                .forEach(record -> payReconciliationRecordRepository.update(record.markRepairDone(remark)));
    }

    private boolean isSyncQueryableChannel(String payChannel) {
        return PayOrder.CHANNEL_MOCK.equals(payChannel) || PayOrder.CHANNEL_ALIPAY_WAP.equals(payChannel) || PayOrder.CHANNEL_ALIPAY_PC.equals(payChannel);
    }

    private PayChannelQueryResult queryChannelForPayReconciliation(PayOrder payOrder) {
        if (payOrder == null) {
            return PayChannelQueryResult.unpaid("LOCAL_PAY_ORDER_MISSING", "");
        }
        try {
            return payChannelClientRouter.route(payOrder.payChannel()).query(payOrder);
        } catch (Exception exception) {
            log.error("Failed to query pay channel for reconciliation, orderNo={}, payOrderNo={}, payChannel={}",
                    payOrder.orderNo(), payOrder.payOrderNo(), payOrder.payChannel(), exception);
            return PayChannelQueryResult.unpaid("CHANNEL_QUERY_FAILED", exception.getMessage());
        }
    }

    private PayChannelQueryResult queryChannelForVerification(PayOrder payOrder) {
        if (payOrder == null) {
            return null;
        }
        try {
            return payChannelClientRouter.route(payOrder.payChannel()).query(payOrder);
        } catch (Exception exception) {
            log.error("Failed to query pay channel for payment exception verification, orderNo={}, payOrderNo={}, payChannel={}",
                    payOrder.orderNo(), payOrder.payOrderNo(), payOrder.payChannel(), exception);
            return PayChannelQueryResult.unpaid("CHANNEL_QUERY_FAILED", exception.getMessage());
        }
    }

    private PayCallbackRecord latestPayCallback(PayOrder payOrder, String orderNo) {
        if (payOrder != null && payOrder.payOrderNo() != null && !payOrder.payOrderNo().isBlank()) {
            return payCallbackRecordRepository.findLatestByOutTradeNo(payOrder.payOrderNo()).orElse(null);
        }
        return orderNo == null || orderNo.isBlank() ? null : payCallbackRecordRepository.findLatestByOutTradeNo(orderNo).orElse(null);
    }

    private VerificationDecision decidePaymentExceptionAction(Order order,
                                                              PayOrder payOrder,
                                                              PayCallbackRecord callbackRecord,
                                                              PayChannelQueryResult channelResult,
                                                              boolean localPaid,
                                                              boolean channelKnown,
                                                              Boolean channelPaid,
                                                              boolean amountConsistent) {
        if (!order.paymentException()) {
            return new VerificationDecision("ORDER_NOT_PAYMENT_EXCEPTION", "MEDIUM", "KEEP_EXCEPTION", List.of(), "当前订单未处于支付异常状态，仅展示核验信息");
        }
        if (!amountConsistent) {
            return new VerificationDecision("AMOUNT_MISMATCH", "HIGH", PayOrderPaymentVerificationView.ACTION_TRANSFER_AMOUNT_RECONCILE, List.of(PayOrderPaymentVerificationView.ACTION_TRANSFER_AMOUNT_RECONCILE), "订单金额与支付单金额不一致，请转入对账处理，禁止直接确认或关闭");
        }
        if (callbackRecord != null && PayCallbackRecord.STATUS_VERIFY_FAILED.equals(callbackRecord.processStatus())) {
            return new VerificationDecision("CALLBACK_VERIFY_FAILED", "HIGH", PayOrderPaymentVerificationView.ACTION_TRANSFER_AMOUNT_RECONCILE, List.of(PayOrderPaymentVerificationView.ACTION_TRANSFER_AMOUNT_RECONCILE), "最近支付回调验签失败，请先核查渠道流水和回调签名，并转入对账处理");
        }
        if (callbackRecord != null && PayCallbackRecord.STATUS_BUSINESS_MISMATCH.equals(callbackRecord.processStatus())) {
            return new VerificationDecision("BUSINESS_MISMATCH", "HIGH", PayOrderPaymentVerificationView.ACTION_TRANSFER_AMOUNT_RECONCILE, List.of(PayOrderPaymentVerificationView.ACTION_TRANSFER_AMOUNT_RECONCILE), "最近支付回调业务身份不匹配，请先进入对账处理");
        }
        if (!localPaid && Boolean.TRUE.equals(channelPaid)) {
            return new VerificationDecision("LOCAL_CHANNEL_STATUS_MISMATCH", "MEDIUM", PayOrderPaymentVerificationView.ACTION_TRANSFER_PAY_SYNC, List.of(PayOrderPaymentVerificationView.ACTION_TRANSFER_PAY_SYNC), "渠道已支付但本地支付单未成功，请转支付管理同步渠道状态，避免订单侧直接人工裁决");
        }
        if (localPaid) {
            return new VerificationDecision("PAID_VERIFIED", "LOW", PayOrderPaymentVerificationView.ACTION_CONFIRM_PAID, List.of(PayOrderPaymentVerificationView.ACTION_CONFIRM_PAID), "本地支付单已成功且金额一致，建议人工确认已支付并恢复履约");
        }
        if (paymentExceptionFromShipped(order)) {
            return new VerificationDecision("SHIPPED_PAYMENT_UNCONFIRMED", "HIGH", PayOrderPaymentVerificationView.ACTION_TRANSFER_AMOUNT_RECONCILE, List.of(PayOrderPaymentVerificationView.ACTION_TRANSFER_AMOUNT_RECONCILE), "该订单已发货后才进入支付异常，不能恢复待支付或关闭释放库存；请转入对账/人工核验，确认支付成功后再恢复已发货履约状态");
        }
        if (payOrder == null) {
            return new VerificationDecision("NO_PAY_ORDER", "LOW", PayOrderPaymentVerificationView.ACTION_RESTORE_PENDING_PAYMENT, List.of(PayOrderPaymentVerificationView.ACTION_RESTORE_PENDING_PAYMENT, PayOrderPaymentVerificationView.ACTION_CLOSE_AND_RELEASE_STOCK), "未查询到支付单且暂无支付成功证据，可恢复待支付；如订单已过支付期且确认无支付痕迹，可关闭并释放库存");
        }
        if (channelKnown && Boolean.FALSE.equals(channelPaid) && !localPaid) {
            if (isChannelFinalUnpaid(channelResult)) {
                return new VerificationDecision("UNPAID_VERIFIED", "LOW", PayOrderPaymentVerificationView.ACTION_CLOSE_AND_RELEASE_STOCK, List.of(PayOrderPaymentVerificationView.ACTION_RESTORE_PENDING_PAYMENT, PayOrderPaymentVerificationView.ACTION_CLOSE_AND_RELEASE_STOCK), "渠道已明确返回关闭或失败，本地也未支付；可按订单有效期和库存情况选择恢复待支付或关闭释放库存");
            }
            return new VerificationDecision("UNPAID_VERIFIED", "LOW", PayOrderPaymentVerificationView.ACTION_RESTORE_PENDING_PAYMENT, List.of(PayOrderPaymentVerificationView.ACTION_RESTORE_PENDING_PAYMENT), "渠道当前为待支付等非成功状态，但尚非最终失败；仅允许恢复待支付继续等待，不允许直接关闭释放库存");
        }
        if (PayOrder.STATUS_CLOSED.equals(payOrder.payStatus()) || PayOrder.STATUS_FAILED.equals(payOrder.payStatus())) {
            return new VerificationDecision("LOCAL_UNPAID", "MEDIUM", PayOrderPaymentVerificationView.ACTION_TRANSFER_AMOUNT_RECONCILE, List.of(PayOrderPaymentVerificationView.ACTION_TRANSFER_AMOUNT_RECONCILE), "本地支付单未成功，但渠道证据不充分；请进入支付单/渠道后台确认未支付，或转对账排查");
        }
        return new VerificationDecision("CHANNEL_UNKNOWN", "MEDIUM", PayOrderPaymentVerificationView.ACTION_TRANSFER_AMOUNT_RECONCILE, List.of(PayOrderPaymentVerificationView.ACTION_TRANSFER_AMOUNT_RECONCILE), "渠道状态未知、查询失败或支付仍处理中；请暂缓处理并稍后重试，必要时转对账排查");
    }

    private boolean paymentExceptionFromShipped(Order order) {
        return order != null
                && order.paymentException()
                && order.remark() != null
                && order.remark().contains("PAYMENT_EXCEPTION_FROM_SHIPPED");
    }

    private boolean isChannelFinalUnpaid(PayChannelQueryResult channelResult) {
        if (channelResult == null || channelResult.tradeStatus() == null) {
            return false;
        }
        String tradeStatus = channelResult.tradeStatus().trim();
        return "CLOSED".equalsIgnoreCase(tradeStatus)
                || "TRADE_CLOSED".equalsIgnoreCase(tradeStatus)
                || "FAILED".equalsIgnoreCase(tradeStatus)
                || "PAY_FAILED".equalsIgnoreCase(tradeStatus);
    }

    private record VerificationDecision(String conclusion, String riskLevel, String suggestedAction, List<String> allowedActions, String message) {}

    private PayOrder compensatePayOrderFromChannel(Order order, PayOrder payOrder, PayChannelQueryResult channelResult) {
        if (payOrder == null || channelResult == null || !channelResult.paid() || payOrder.success()) {
            return payOrder;
        }
        if (!payOrder.amountConsistentWith(order.payAmountCent())) {
            log.error("Skipped pay order compensation because amount mismatched, orderNo={}, payOrderNo={}, orderAmountCent={}, payAmountCent={}, tradeStatus={}",
                    order.orderNo(), payOrder.payOrderNo(), order.payAmountCent(), payOrder.payAmountCent(), channelResult.tradeStatus());
            return payOrder;
        }
        if (!order.pendingPayment() && !order.paidOrAfter()) {
            log.error("Skipped pay order compensation because order is not payable, orderNo={}, orderStatus={}, payOrderNo={}, tradeStatus={}",
                    order.orderNo(), order.orderStatus(), payOrder.payOrderNo(), channelResult.tradeStatus());
            return payOrder;
        }
        PayOrder successOrder = payOrder.markSuccess(channelResult.transactionNo(), LocalDateTime.now());
        payOrderDomainService.update(successOrder);
        log.warn("Compensated pay order success from channel reconciliation, orderNo={}, payOrderNo={}, payChannel={}, tradeNo={}, tradeStatus={}",
                order.orderNo(), successOrder.payOrderNo(), successOrder.payChannel(), successOrder.transactionNo(), channelResult.tradeStatus());
        return successOrder;
    }

    private Order compensateOrderFromPayOrder(Order order, PayOrder payOrder, PayChannelQueryResult channelResult) {
        if (payOrder == null || !payOrder.success() || !order.pendingPayment()) {
            return order;
        }
        if (!payOrder.amountConsistentWith(order.payAmountCent())) {
            log.error("Skipped order paid compensation because amount mismatched, orderNo={}, payOrderNo={}, orderAmountCent={}, payAmountCent={}",
                    order.orderNo(), payOrder.payOrderNo(), order.payAmountCent(), payOrder.payAmountCent());
            return order;
        }
        orderFacade.markPaid(order.orderNo());
        Order compensated = orderFacade.getByOrderNo(order.orderNo());
        log.warn("Compensated order paid from pay reconciliation, orderNo={}, payOrderNo={}, channelPaid={}, tradeStatus={}",
                order.orderNo(), payOrder.payOrderNo(), channelResult != null && channelResult.paid(), channelResult == null ? null : channelResult.tradeStatus());
        return compensated;
    }

    private boolean payStatusConsistent(Order order, PayOrder payOrder, PayChannelQueryResult channelResult) {
        if (payOrder == null) {
            return order.pendingPayment();
        }
        boolean localConsistent = payOrderDomainService.reconcileStatus(order, payOrder);
        if (channelResult == null) {
            return localConsistent;
        }
        if (channelResult.paid()) {
            return localConsistent && payOrder.success() && order.paidOrAfter();
        }
        if (payOrder.success() || order.paidOrAfter()) {
            return false;
        }
        return localConsistent;
    }

    private void recordPayReconciliation(String batchNo,
                                         Order order,
                                         PayOrder payOrder,
                                         PayChannelQueryResult channelResult,
                                         boolean amountConsistent,
                                         boolean statusConsistent,
                                         boolean consistent) {
        String diffType = resolvePayReconcileDiffType(order, payOrder, channelResult, amountConsistent, statusConsistent, consistent);
        PayReconciliationRecord record;
        if (PayReconciliationRecord.DIFF_NONE.equals(diffType)) {
            record = PayReconciliationRecord.pay(new PayReconciliationRecord.OrderPayReconciliationSnapshot(
                    order.orderNo(),
                    payOrder == null ? null : payOrder.payOrderNo(),
                    payOrder == null ? null : payOrder.payStatus(),
                    channelPayStatus(order, payOrder, channelResult),
                    payOrder == null ? null : payOrder.payAmountCent(),
                    order.payAmountCent(),
                    payOrder != null,
                    true,
                    true,
                    buildPayReconcileRemark(batchNo, order, payOrder, channelResult, true)
            ));
        } else {
            record = PayReconciliationRecord.suspicious(new PayReconciliationRecord.OrderRepairPendingSnapshot(
                    order.orderNo(),
                    payOrder == null ? null : payOrder.payOrderNo(),
                    payOrder == null ? order.orderStatus() : order.orderStatus() + "/" + payOrder.payStatus(),
                    channelPayStatus(order, payOrder, channelResult),
                    order.payAmountCent(),
                    payOrder == null ? null : payOrder.payAmountCent(),
                    buildPayReconcileRemark(batchNo, order, payOrder, channelResult, false)
            ), diffType);
        }
        upsertActivePayReconciliation(record.withBatchNo(batchNo));
    }

    private void upsertActivePayReconciliation(PayReconciliationRecord nextRecord) {
        if (nextRecord.consistent()) {
            payReconciliationRecordRepository.save(nextRecord);
            return;
        }
        List<PayReconciliationRecord> activeRecords = payReconciliationRecordRepository.findPendingByOrderNoAndDiffType(nextRecord.orderNo(), nextRecord.diffType(), 1);
        if (activeRecords.isEmpty()) {
            payReconciliationRecordRepository.save(nextRecord);
            return;
        }
        payReconciliationRecordRepository.update(activeRecords.get(0).refreshFrom(nextRecord));
    }

    private String resolvePayReconcileDiffType(Order order,
                                               PayOrder payOrder,
                                               PayChannelQueryResult channelResult,
                                               boolean amountConsistent,
                                               boolean statusConsistent,
                                               boolean consistent) {
        if (consistent) {
            return PayReconciliationRecord.DIFF_NONE;
        }
        boolean orderPaid = order.paidOrAfter();
        boolean payExists = payOrder != null;
        boolean paySuccess = payOrder != null && payOrder.success();
        boolean channelKnown = channelResult != null && !"CHANNEL_QUERY_FAILED".equalsIgnoreCase(channelResult.tradeStatus());
        boolean channelPaid = channelResult != null && channelResult.paid();
        if (!payExists && orderPaid) {
            return PayReconciliationRecord.DIFF_SHORT_PAID_ORDER_PAY_MISSING;
        }
        if (paySuccess && channelKnown && !channelPaid) {
            return PayReconciliationRecord.DIFF_LONG_PLATFORM_SUCCESS_CHANNEL_MISSING;
        }
        if (channelPaid && (!paySuccess || !orderPaid)) {
            return PayReconciliationRecord.DIFF_SHORT_CHANNEL_SUCCESS_PLATFORM_UNPAID;
        }
        if (paySuccess && !orderPaid) {
            return PayReconciliationRecord.DIFF_ORDER_STATUS_NOT_SYNCED;
        }
        if (!amountConsistent && !statusConsistent) {
            return PayReconciliationRecord.DIFF_STATUS_AND_AMOUNT_MISMATCH;
        }
        if (!amountConsistent) {
            return PayReconciliationRecord.DIFF_AMOUNT_MISMATCH;
        }
        if (!statusConsistent) {
            return PayReconciliationRecord.DIFF_STATUS_MISMATCH;
        }
        return PayReconciliationRecord.DIFF_UNKNOWN;
    }

    private String channelPayStatus(Order order, PayOrder payOrder, PayChannelQueryResult channelResult) {
        if (channelResult == null) {
            return order.orderStatus();
        }
        if (channelResult.paid()) {
            return PayOrder.STATUS_SUCCESS;
        }
        return channelResult.tradeStatus() == null || channelResult.tradeStatus().isBlank() ? "CHANNEL_UNPAID" : channelResult.tradeStatus();
    }

    private String buildPayReconcileRemark(String batchNo, Order order, PayOrder payOrder, PayChannelQueryResult channelResult, boolean consistent) {
        if (consistent) {
            return "批次=" + batchNo + "，支付订单、业务订单与渠道状态一致";
        }
        if (payOrder == null) {
            return "批次=" + batchNo + "，平台订单已存在但缺少支付单，orderStatus=" + order.orderStatus() + "，需确认是否误置已支付或需重新支付";
        }
        boolean channelKnown = channelResult != null && !"CHANNEL_QUERY_FAILED".equalsIgnoreCase(channelResult.tradeStatus());
        return "批次=" + batchNo + ",orderStatus=" + order.orderStatus()
                + ",payStatus=" + payOrder.payStatus()
                + ",channelKnown=" + channelKnown
                + ",channelPaid=" + (channelResult != null && channelResult.paid())
                + ",channelStatus=" + (channelResult == null ? null : channelResult.tradeStatus())
                + ",localAmountCent=" + payOrder.payAmountCent()
                + ",orderAmountCent=" + order.payAmountCent();
    }

    private void recordRefundReconciliation(PayOrder payOrder,
                                            PayRefundOrder refundOrder,
                                            String refundNo,
                                            Long refundAmountCent,
                                            String channelStatus,
                                            boolean channelKnown,
                                            String remark) {
        String batchNo = newReconcileBatchNo(PayReconciliationRecord.BIZ_TYPE_REFUND);
        Long localAmount = refundOrder == null ? refundAmountCent : refundOrder.refundAmountCent();
        Long channelAmount = refundAmountCent == null ? localAmount : refundAmountCent;
        String localStatus = refundOrder == null ? payOrder.payStatus() : refundOrder.refundStatus();
        boolean amountConsistent = localAmount == null || channelAmount == null || localAmount.equals(channelAmount);
        boolean statusConsistent = refundStatusConsistent(localStatus, channelStatus);
        payReconciliationRecordRepository.save(PayReconciliationRecord.refund(new PayReconciliationRecord.RefundReconciliationSnapshot(
                payOrder.orderNo(),
                payOrder.payOrderNo(),
                refundNo,
                localStatus,
                channelStatus,
                localAmount,
                channelAmount,
                refundOrder != null,
                channelKnown,
                statusConsistent,
                amountConsistent,
                appendBatchRemark(batchNo, remark)
        )).withBatchNo(batchNo));
    }

    private void recordRefundCallback(PayOrder payOrder,
                                      String refundNo,
                                      String channelRefundNo,
                                      Long refundAmountCent,
                                      String tradeStatus,
                                      String rawPayload,
                                      boolean processed,
                                      String failReason) {
        PayCallbackRecord callbackRecord = payCallbackRecordRepository.save(PayCallbackRecord.createRefund(
                payOrder.payChannel(),
                payOrder.payOrderNo(),
                refundNo,
                payOrder.orderNo(),
                refundNo,
                channelRefundNo,
                refundAmountCent,
                tradeStatus,
                "REFUND-SYNC",
                true,
                rawPayload,
                LocalDateTime.now()
        ));
        payCallbackRecordRepository.update(processed
                ? callbackRecord.markProcessed()
                : callbackRecord.markProcessFailed(failReason == null || failReason.isBlank() ? "退款处理失败" : failReason));
    }

    private String newReconcileBatchNo(String bizType) {
        return "RC" + bizType + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String appendBatchRemark(String batchNo, String remark) {
        return "批次=" + batchNo + (remark == null || remark.isBlank() ? "" : "，" + remark);
    }

    private boolean refundStatusConsistent(String localStatus, String channelStatus) {
        if (channelStatus == null || channelStatus.isBlank()) {
            return false;
        }
        if (PayRefundOrder.STATUS_SUCCESS.equals(channelStatus)) {
            return PayRefundOrder.STATUS_SUCCESS.equals(localStatus) || PayOrder.STATUS_REFUNDED.equals(localStatus);
        }
        if (PayRefundOrder.STATUS_FAILED.equals(channelStatus)) {
            return PayRefundOrder.STATUS_FAILED.equals(localStatus) || PayOrder.STATUS_REFUND_FAILED.equals(localStatus);
        }
        return channelStatus.equals(localStatus);
    }

    private PayChannelQueryResult queryChannelBeforeClosing(PayOrder payOrder, String reasonStatus) {
        try {
            return payChannelClientRouter.route(payOrder.payChannel()).query(payOrder);
        } catch (Exception exception) {
            log.error("Skipped closing pay order because channel query failed before timeout close, orderNo={}, payOrderNo={}, reasonStatus={}",
                    payOrder.orderNo(), payOrder.payOrderNo(), reasonStatus, exception);
            return PayChannelQueryResult.paid("", "CHANNEL_QUERY_FAILED", exception.getMessage());
        }
    }

    private void handlePaidChannelResultBeforeClose(PayOrder payOrder, PayChannelQueryResult channelResult, String reasonStatus) {
        try {
            Order order = orderFacade.getByOrderNo(payOrder.orderNo());
            PayOrder successOrder = compensatePayOrderFromChannel(order, payOrder, channelResult);
            compensateOrderFromPayOrder(order, successOrder, channelResult);
            recordPayReconciliation(newReconcileBatchNo(PayReconciliationRecord.BIZ_TYPE_PAY), orderFacade.getByOrderNo(payOrder.orderNo()), successOrder, channelResult,
                    successOrder == null || successOrder.amountConsistentWith(order.payAmountCent()),
                    successOrder != null && successOrder.success(),
                    successOrder != null && successOrder.success() && successOrder.amountConsistentWith(order.payAmountCent()));
            log.warn("Prevented closing pay order because channel reports paid, orderNo={}, payOrderNo={}, reasonStatus={}, tradeStatus={}",
                    payOrder.orderNo(), payOrder.payOrderNo(), reasonStatus, channelResult.tradeStatus());
        } catch (Exception exception) {
            log.error("Failed to compensate paid channel result before closing pay order, orderNo={}, payOrderNo={}, reasonStatus={}",
                    payOrder.orderNo(), payOrder.payOrderNo(), reasonStatus, exception);
        }
    }

    public PayOrderView refund(String orderNo, Long refundAmountCent, String reason, String refundNo) {
        PayOrder payOrder = payOrderDomainService.loadByOrderNo(orderNo);
        if (payOrder.refunded()) {
            return payViewAssembler.toView(payOrder);
        }
        if (!payOrder.success() && !PayOrder.STATUS_REFUND_FAILED.equals(payOrder.payStatus())) {
            throw BusinessException.badRequest("当前支付单状态不允许发起退款");
        }
        long safeRefundAmount = refundAmountCent == null ? payOrder.payAmountCent() : refundAmountCent;
        if (safeRefundAmount <= 0 || safeRefundAmount > payOrder.payAmountCent()) {
            throw BusinessException.badRequest("退款金额不合法");
        }
        String safeRefundNo = refundNo == null || refundNo.isBlank() ? newRefundNo(orderNo) : refundNo;
        PayRefundOrder refundOrder = payRefundOrderRepository.findByRefundNo(safeRefundNo)
                .orElseGet(() -> payRefundOrderRepository.save(PayRefundOrder.create(payOrder, safeRefundNo, safeRefundAmount, reason)));
        PayOrder refundPending = payOrder.markRefundPending(LocalDateTime.now(), reason);
        payOrderDomainService.update(refundPending);
        PayOrder refunding = refundPending.markRefunding(LocalDateTime.now(), reason);
        payOrderDomainService.update(refunding);
        if (!PayRefundOrder.STATUS_REFUNDING.equals(refundOrder.refundStatus())) {
            payRefundOrderRepository.update(refundOrder.markRefunding());
        }
        log.info("Created refund order waiting for channel sync, orderNo={}, payOrderNo={}, refundNo={}, amountCent={}",
                orderNo, refunding.payOrderNo(), safeRefundNo, safeRefundAmount);
        return payViewAssembler.toView(payOrderDomainService.loadByOrderNo(orderNo));
    }

    public PayOrderView reconcileRefund(String orderNo, String refundNo, Long refundAmountCent) {
        PayOrder payOrder = payOrderDomainService.loadByOrderNo(orderNo);
        PayRefundOrder refundOrder = refundNo == null || refundNo.isBlank()
                ? null
                : payRefundOrderRepository.findByRefundNo(refundNo).orElse(null);
        if (payOrder.refunded() && !PayOrder.STATUS_REFUND_PENDING.equals(payOrder.payStatus()) && !PayOrder.STATUS_REFUNDING.equals(payOrder.payStatus())) {
            recordRefundReconciliation(payOrder, refundOrder, refundNo, refundAmountCent, PayRefundOrder.STATUS_SUCCESS, true, "本地退款已成功，无需渠道补偿查询");
            return payViewAssembler.toView(payOrder);
        }
        if (refundOrder != null && PayRefundOrder.STATUS_REFUNDING.equals(refundOrder.refundStatus())) {
            long safeRefundAmount = refundAmountCent == null ? refundOrder.refundAmountCent() : refundAmountCent;
            PayRefundResult refundResult = payChannelClientRouter.route(payOrder.payChannel())
                    .refund(new PayRefundRequest(payOrder, refundNo, safeRefundAmount, "refund_sync"));
            if (refundResult.success()) {
                PayOrder refunded = payOrder.markRefundSuccess(LocalDateTime.now(), "refund_sync", safeRefundAmount);
                payOrderDomainService.update(refunded);
                payRefundOrderRepository.update(refundOrder.markSuccess(refundResult.channelRefundNo(), refundResult.rawPayload()));
                recordRefundCallback(refunded, refundNo, refundResult.channelRefundNo(), safeRefundAmount, PayRefundOrder.STATUS_SUCCESS, refundResult.rawPayload(), true, null);
                recordRefundReconciliation(refunded, refundOrder, refundNo, safeRefundAmount, PayRefundOrder.STATUS_SUCCESS, true, refundResult.message());
                payRefundResultPublisher.publishSucceeded(orderNo, refundNo, safeRefundAmount, refundResult.channelRefundNo());
                log.warn("Synced refund success from channel refund, orderNo={}, refundNo={}, channelRefundNo={}",
                        orderNo, refundNo, refundResult.channelRefundNo());
                return payViewAssembler.toView(payOrderDomainService.loadByOrderNo(orderNo));
            }
            PayOrder failed = payOrder.markRefundFailed(LocalDateTime.now(), refundResult.message());
            payOrderDomainService.update(failed);
            payRefundOrderRepository.update(refundOrder.markFailed(refundResult.message(), refundResult.rawPayload()));
            recordRefundCallback(failed, refundNo, null, safeRefundAmount, PayRefundOrder.STATUS_FAILED, refundResult.rawPayload(), false, refundResult.message());
            recordRefundReconciliation(failed, refundOrder, refundNo, safeRefundAmount, PayRefundOrder.STATUS_FAILED, true, refundResult.message());
            payRefundResultPublisher.publishFailed(orderNo, refundNo, safeRefundAmount, refundResult.message());
            log.warn("Synced refund failed from channel refund, orderNo={}, refundNo={}, reason={}",
                    orderNo, refundNo, refundResult.message());
            return payViewAssembler.toView(payOrderDomainService.loadByOrderNo(orderNo));
        }
        PayRefundQueryResult queryResult = payChannelClientRouter.route(payOrder.payChannel())
                .queryRefund(new PayRefundQueryRequest(payOrder, refundNo));
        if (queryResult.success()) {
            long safeRefundAmount = refundAmountCent == null ? payOrder.payAmountCent() : refundAmountCent;
            PayOrder refunded = payOrder.markRefundSuccess(LocalDateTime.now(), "refund_reconcile", safeRefundAmount);
            payOrderDomainService.update(refunded);
            if (refundOrder != null) {
                payRefundOrderRepository.update(refundOrder.markSuccess(queryResult.channelRefundNo(), queryResult.rawPayload()));
            }
            recordRefundCallback(refunded, refundNo, queryResult.channelRefundNo(), safeRefundAmount, PayRefundOrder.STATUS_SUCCESS, queryResult.rawPayload(), true, null);
            recordRefundReconciliation(refunded, refundOrder, refundNo, safeRefundAmount, PayRefundOrder.STATUS_SUCCESS, true, queryResult.message());
            payRefundResultPublisher.publishSucceeded(orderNo, refundNo, safeRefundAmount, queryResult.channelRefundNo());
            log.warn("Reconciled refund success from channel query, orderNo={}, refundNo={}, channelRefundNo={}",
                    orderNo, refundNo, queryResult.channelRefundNo());
            return payViewAssembler.toView(payOrderDomainService.loadByOrderNo(orderNo));
        }
        if (queryResult.failed()) {
            PayOrder failed = payOrder.markRefundFailed(LocalDateTime.now(), queryResult.message());
            payOrderDomainService.update(failed);
            if (refundOrder != null) {
                payRefundOrderRepository.update(refundOrder.markFailed(queryResult.message(), queryResult.rawPayload()));
            }
            recordRefundCallback(failed, refundNo, queryResult.channelRefundNo(), refundAmountCent, PayRefundOrder.STATUS_FAILED, queryResult.rawPayload(), false, queryResult.message());
            recordRefundReconciliation(failed, refundOrder, refundNo, refundAmountCent, PayRefundOrder.STATUS_FAILED, true, queryResult.message());
            payRefundResultPublisher.publishFailed(orderNo, refundNo, refundAmountCent, queryResult.message());
            log.warn("Reconciled refund failed from channel query, orderNo={}, refundNo={}, reason={}",
                    orderNo, refundNo, queryResult.message());
            return payViewAssembler.toView(payOrderDomainService.loadByOrderNo(orderNo));
        }
        recordRefundReconciliation(payOrder, refundOrder, refundNo, refundAmountCent, queryResult.tradeStatus(), false, queryResult.message());
        log.info("Refund reconcile result unknown, orderNo={}, refundNo={}, tradeStatus={}, message={}",
                orderNo, refundNo, queryResult.tradeStatus(), queryResult.message());
        return payViewAssembler.toView(payOrder);
    }

    public PayOrderView mockRefund(String orderNo, String reason) {
        PayOrder payOrder = payOrderDomainService.loadByOrderNo(orderNo);
        return refund(orderNo, payOrder.payAmountCent(), reason, newRefundNo(orderNo));
    }

    public PayOrderView closePayOrder(String orderNo, String reason) {
        PayOrder payOrder = payOrderDomainService.loadByOrderNo(orderNo);
        payOrderDomainService.update(payOrder.close(reason));
        return payViewAssembler.toView(payOrderDomainService.loadByOrderNo(orderNo));
    }

    public void closePendingPayOrders(String orderNo, String reasonStatus) {
        List<PayOrder> pendingPayOrders = payOrderDomainService.loadPendingByOrderNo(orderNo);
        for (PayOrder payOrder : pendingPayOrders) {
            PayChannelQueryResult channelResult = queryChannelBeforeClosing(payOrder, reasonStatus);
            if (channelResult.paid()) {
                handlePaidChannelResultBeforeClose(payOrder, channelResult, reasonStatus);
                continue;
            }
            payOrderDomainService.update(payOrder.closeByOrderStatus(reasonStatus));
        }
    }

    public PayOrderView detail(String payOrderNo) {
        return payViewAssembler.toView(payOrderDomainService.loadByPayOrderNo(payOrderNo));
    }

    public String renderPaySubmitPage(String payOrderNo, String returnPath) {
        PayOrder payOrder = payOrderDomainService.loadByPayOrderNo(payOrderNo);
        orderFacade.closeIfTimedOut(payOrder.orderNo());
        Order order = orderFacade.getByOrderNo(payOrder.orderNo());
        ensureOrderPayable(order);
        if (!payOrder.pending()) {
            throw BusinessException.badRequest("当前支付单状态不允许继续支付");
        }
        PayChannelSubmitResult submitResult = buildSubmitResult(payOrder, returnPath, remainingPayTime(order));
        PayOrder submittedOrder = payOrder.withSubmission(submitResult.displayPayload());
        payOrderDomainService.update(submittedOrder);
        return buildSubmitPage(submitResult);
    }

    public String renderAlipayReturnBridge(Map<String, String> params) {
        String payOrderNo = valueOrEmpty(params.get("out_trade_no"));
        String orderNo = valueOrEmpty(params.get("passback_params"));
        String tradeStatus = valueOrEmpty(params.get("trade_status"));
        String totalAmount = valueOrEmpty(params.get("total_amount"));
        String sign = valueOrEmpty(params.get("sign"));
        String returnPath = normalizeReturnPath(params.get("returnPath"));
        String payChannel = PayOrder.CHANNEL_ALIPAY_PC;
        if (!payOrderNo.isBlank()) {
            PayOrder payOrder = payOrderRepository.findByPayOrderNo(payOrderNo).orElse(null);
            if (payOrder != null) {
                payChannel = payOrder.payChannel();
                orderNo = payOrder.orderNo();
            }
        }

        if (!payOrderNo.isBlank() && !tradeStatus.isBlank() && !sign.isBlank()) {
            try {
                PayChannelCallbackRequest request = new PayChannelCallbackRequest(
                        payChannel,
                        payOrderNo,
                        orderNo,
                        payOrderNo,
                        valueOrEmpty(params.get("trade_no")),
                        sign,
                        buildAlipayRawPayload(params),
                        tradeStatus
                );
                handleChannelCallback(request);
                log.info("Processed Alipay return bridge as verified callback fallback, orderNo={}, payOrderNo={}, tradeNo={}, tradeStatus={}, payChannel={}",
                        orderNo, payOrderNo, request.transactionNo(), tradeStatus, payChannel);
            } catch (Exception exception) {
                log.warn("Failed to process Alipay return bridge as callback fallback, orderNo={}, payOrderNo={}, tradeStatus={}, payChannel={}, message={}",
                        orderNo, payOrderNo, tradeStatus, payChannel, exception.getMessage());
            }
        }

        String targetUrl = buildClientReturnUrl()
                + querySeparator(buildClientReturnUrl())
                + "payOrderNo=" + urlEncode(payOrderNo)
                + "&orderNo=" + urlEncode(orderNo)
                + "&payChannel=" + urlEncode(payChannel)
                + "&returnPath=" + urlEncode(returnPath)
                + "&tradeStatus=" + urlEncode(tradeStatus)
                + "&totalAmount=" + urlEncode(totalAmount)
                + "&sign=" + urlEncode(sign);
        return buildReturnBridgePage(targetUrl, returnPath, payOrderNo, orderNo, payChannel, tradeStatus);
    }

    public void ensureRefundable(String orderNo) {
        orderFacade.ensureRefundable(orderNo);
    }

    private String buildSubmitPage(PayChannelSubmitResult submitResult) {
        String redirectForm = submitResult.redirectForm();
        String redirectUrl = submitResult.redirectUrl();
        if ((redirectForm == null || redirectForm.isBlank()) && (redirectUrl == null || redirectUrl.isBlank())) {
            throw BusinessException.badRequest("支付提交信息为空，无法跳转支付渠道");
        }
        if (redirectForm != null && redirectForm.contains("<form")) {
            return "<!doctype html><html><head><meta charset=\"UTF-8\"><title>正在跳转支付宝</title></head>"
                    + "<body><div style=\"padding:24px;font-family:Arial,sans-serif;color:#334155;\">正在跳转支付宝，请稍候...</div>"
                    + redirectForm
                    + "<script>setTimeout(function(){var f=document.querySelector('form');if(f){f.submit();}},100);</script>"
                    + "</body></html>";
        }
        String target = (redirectUrl != null && !redirectUrl.isBlank()) ? redirectUrl : redirectForm;
        if (PayOrder.CHANNEL_ALIPAY_PC.equals(submitResult.channelCode())) {
            return buildPcSandboxPromptPage(target);
        }
        return "<!doctype html><html><head><meta charset=\"UTF-8\"><title>正在跳转支付宝</title></head>"
                + "<body><div style=\"padding:24px;font-family:Arial,sans-serif;color:#334155;\">正在跳转支付宝，请稍候...</div>"
                + "<div style=\"padding:0 24px 24px;word-break:break-all;\"><a href=\"" + htmlEscape(target) + "\">如未自动跳转，请点此继续</a></div>"
                + "<script>setTimeout(function(){window.location.replace('" + jsEscape(target) + "');},100);</script>"
                + "</body></html>";
    }

    private String buildPcSandboxPromptPage(String targetUrl) {
        String escapedTargetUrl = htmlEscape(targetUrl);
        return "<!doctype html><html><head><meta charset=\"UTF-8\"><title>前往支付宝沙箱</title></head>"
                + "<body style=\"margin:0;background:linear-gradient(180deg,#f8fbff 0%,#f6f8fb 100%);font-family:Arial,sans-serif;color:#0f172a;\">"
                + "<div style=\"max-width:560px;margin:56px auto;padding:28px 24px;background:#fff;border-radius:20px;box-shadow:0 10px 30px rgba(15,23,42,.08);\">"
                + "<h2 style=\"margin:0 0 12px;font-size:24px;\">正在前往支付宝 PC 沙箱</h2>"
                + "<div style=\"font-size:14px;line-height:1.8;color:#475569;\">若登录支付宝沙箱账号后首次进入收银台出现错误页，请直接刷新一次当前支付宝页面。该操作不会重复创建支付单，也不会重复扣款。</div>"
                + "<div style=\"margin-top:16px;padding:14px 16px;border-radius:14px;background:#eff6ff;color:#1d4ed8;font-size:13px;line-height:1.8;\">提示：这是支付宝沙箱环境的偶发现象，刷新后通常可恢复正常支付。</div>"
                + "<div style=\"margin-top:18px;word-break:break-all;\"><a href=\"" + escapedTargetUrl + "\" style=\"color:#2563eb;text-decoration:none;\">如未自动跳转，请点此继续前往支付宝</a></div>"
                + "<div style=\"margin-top:18px;font-size:12px;color:#94a3b8;\">页面将在 1.2 秒后自动继续。</div>"
                + "</div>"
                + "<script>(function(){var target='" + jsEscape(targetUrl) + "';setTimeout(function(){window.location.replace(target);},1200);})();</script>"
                + "</body></html>";
    }

    private String buildReturnBridgePage(String targetUrl, String returnPath, String payOrderNo, String orderNo, String payChannel, String tradeStatus) {
        String escapedTargetUrl = htmlEscape(targetUrl);
        String escapedReturnPath = htmlEscape(returnPath);
        String escapedPayOrderNo = htmlEscape(payOrderNo);
        String escapedOrderNo = htmlEscape(orderNo);
        String escapedPayChannel = htmlEscape(payChannel);
        String escapedTradeStatus = htmlEscape(tradeStatus);
        return "<!doctype html><html><head><meta charset=\"UTF-8\"><title>支付结果处理中</title></head>"
                + "<body style=\"margin:0;background:#f8fafc;font-family:Arial,sans-serif;color:#0f172a;\">"
                + "<div style=\"max-width:560px;margin:40px auto;padding:24px;background:#fff;border-radius:18px;box-shadow:0 10px 30px rgba(15,23,42,.08);\">"
                + "<h2 style=\"margin:0 0 12px;\">支付结果处理中</h2>"
                + "<div style=\"font-size:14px;line-height:1.8;color:#475569;\">正在同步支付结果并进入结果页，请稍候...</div>"
                + "<div style=\"margin-top:16px;font-size:13px;line-height:1.8;color:#64748b;word-break:break-all;\">"
                + "<div>订单号：" + escapedOrderNo + "</div>"
                + "<div>支付单号：" + escapedPayOrderNo + "</div>"
                + "<div>支付渠道：" + escapedPayChannel + "</div>"
                + "<div>交易状态：" + escapedTradeStatus + "</div>"
                + "</div>"
                + "<div style=\"margin-top:18px;\"><a href=\"" + escapedTargetUrl + "\" style=\"color:#2563eb;text-decoration:none;\">如未自动跳转，请点此继续</a></div>"
                + "</div>"
                + "<script>(function(){var target='" + jsEscape(targetUrl) + "';setTimeout(function(){window.location.replace(target);},120);})();</script>"
                + "</body></html>";
    }

    private PayChannelSubmitResult buildSubmitResult(PayOrder payOrder, String returnPath) {
        return buildSubmitResult(payOrder, returnPath, null);
    }

    private PayChannelSubmitResult buildSubmitResult(PayOrder payOrder, String returnPath, Duration remainingPayTime) {
        if (PayOrder.CHANNEL_ALIPAY_WAP.equals(payOrder.payChannel())) {
            String resolvedReturnUrl = buildReturnUrl(payOrder, returnPath);
            return payChannelClientRouter.route(payOrder.payChannel()).submit(payOrder, resolvedReturnUrl, remainingPayTime);
        }
        return payChannelClientRouter.route(payOrder.payChannel()).submit(payOrder, buildBaseReturnUrl(), remainingPayTime);
    }

    private void ensureOrderPayable(Order order) {
        if (order.paymentException()) {
            throw BusinessException.badRequest("订单支付状态异常，请联系平台客服处理");
        }
        if (!order.pendingPayment()) {
            throw BusinessException.badRequest("当前订单状态不允许支付");
        }
        if (order.timedOut(LocalDateTime.now())) {
            throw BusinessException.badRequest("订单已超时，请重新下单");
        }
        if (order.remainingPaySeconds(LocalDateTime.now()) < 90) {
            throw BusinessException.badRequest("订单即将超时，请重新下单");
        }
    }

    private Duration remainingPayTime(Order order) {
        return Duration.between(LocalDateTime.now(), order.effectiveExpireTime(0));
    }

    private String buildReturnUrl(PayOrder payOrder, String returnPath) {
        String baseReturnUrl = buildBaseReturnUrl();
        String separator = baseReturnUrl.contains("?") ? "&" : "?";
        return baseReturnUrl + separator
                + "payOrderNo=" + urlEncode(payOrder.payOrderNo())
                + "&orderNo=" + urlEncode(payOrder.orderNo())
                + "&payChannel=" + urlEncode(payOrder.payChannel())
                + "&returnPath=" + urlEncode(normalizeReturnPath(returnPath));
    }

    private String buildBaseReturnUrl() {
        String returnUrl = alipaySandboxProperties.getReturnUrl();
        if (returnUrl == null || returnUrl.isBlank()) {
            throw BusinessException.badRequest("支付宝回跳地址未配置，请检查 mall.pay.alipay.return-url");
        }
        return returnUrl;
    }

    private String buildClientReturnUrl() {
        String clientReturnUrl = alipaySandboxProperties.getClientReturnUrl();
        if (clientReturnUrl == null || clientReturnUrl.isBlank()) {
            throw BusinessException.badRequest("支付宝前端回跳地址未配置，请检查 mall.pay.alipay.client-return-url");
        }
        return clientReturnUrl;
    }

    private String querySeparator(String url) {
        return url.contains("?") ? "&" : "?";
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String htmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String jsEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("</", "<\\/");
    }

    private String normalizeReturnPath(String returnPath) {
        if (returnPath == null || returnPath.isBlank()) {
            return "/orders";
        }
        if (returnPath.startsWith("http://") || returnPath.startsWith("https://")) {
            return "/orders";
        }
        return returnPath.startsWith("/") ? returnPath : "/" + returnPath;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private PayOrder findReusablePayOrder(String orderNo, String payChannel) {
        return payOrderRepository.findByOrderNoList(orderNo).stream()
                .filter(PayOrder::pending)
                .filter(payOrder -> payOrder.sameChannel(payChannel))
                .findFirst()
                .orElse(null);
    }

    private void closeOtherPendingPayOrders(String orderNo, String currentChannel) {
        for (PayOrder existing : payOrderRepository.findByOrderNoList(orderNo)) {
            if (!existing.pending() || existing.sameChannel(currentChannel)) {
                continue;
            }
            PayOrder closed = existing.close("switch_channel_to_" + currentChannel);
            payOrderRepository.update(closed);
            log.info("Closed pending pay order before switching channel, orderNo={}, payOrderNo={}, fromChannel={}, toChannel={}",
                    orderNo, existing.payOrderNo(), existing.payChannel(), currentChannel);
        }
    }

    private PayOrder locatePayOrderForCallback(String orderNo, String payOrderNo) {
        if (payOrderNo != null && !payOrderNo.isBlank()) {
            return payOrderDomainService.loadByPayOrderNo(payOrderNo);
        }
        return requireLatestPayOrder(orderNo);
    }

    private PayOrder requireLatestPayOrder(String orderNo) {
        return payOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> BusinessException.badRequest("支付单不存在"));
    }

    private String resolvePayChannel(String payChannel) {
        return payChannel == null || payChannel.isBlank() ? PayOrder.CHANNEL_MOCK : payChannel;
    }

    private String buildAlipayRawPayload(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .filter(entry -> !"sign".equals(entry.getKey()) && !"sign_type".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private Long parseAlipayAmountCent(String amount) {
        if (amount == null || amount.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(amount).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            log.warn("Failed to parse Alipay amount, amount={}", amount, exception);
            return null;
        }
    }

    private String newPayOrderNo() {
        return "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String newRefundNo(String orderNo) {
        return "REF" + System.currentTimeMillis() + "-" + orderNo + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
