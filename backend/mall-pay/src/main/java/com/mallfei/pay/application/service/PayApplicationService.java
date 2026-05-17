package com.mallfei.pay.application.service;

import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.facade.OrderFacade;
import com.mallfei.pay.application.assembler.PayViewAssembler;
import com.mallfei.pay.application.vo.PayOrderView;
import com.mallfei.pay.application.vo.PayReconcileResultView;
import com.mallfei.pay.config.AlipaySandboxProperties;
import com.mallfei.pay.domain.model.PayCallbackRecord;
import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.pay.domain.repository.PayCallbackRecordRepository;
import com.mallfei.pay.domain.repository.PayOrderRepository;
import com.mallfei.pay.domain.service.PayChannelCallbackRequest;
import com.mallfei.pay.domain.service.PayChannelClientRouter;
import com.mallfei.pay.domain.service.PayChannelQueryResult;
import com.mallfei.pay.domain.service.PayChannelSubmitResult;
import com.mallfei.pay.domain.service.PayOrderDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final PayOrderDomainService payOrderDomainService;
    private final OrderFacade orderFacade;
    private final PayViewAssembler payViewAssembler;
    private final PayChannelClientRouter payChannelClientRouter;
    private final AlipaySandboxProperties alipaySandboxProperties;

    public PayApplicationService(PayOrderRepository payOrderRepository,
                                 PayCallbackRecordRepository payCallbackRecordRepository,
                                 PayOrderDomainService payOrderDomainService,
                                 OrderFacade orderFacade,
                                 PayViewAssembler payViewAssembler,
                                 PayChannelClientRouter payChannelClientRouter,
                                 AlipaySandboxProperties alipaySandboxProperties) {
        this.payOrderRepository = payOrderRepository;
        this.payCallbackRecordRepository = payCallbackRecordRepository;
        this.payOrderDomainService = payOrderDomainService;
        this.orderFacade = orderFacade;
        this.payViewAssembler = payViewAssembler;
        this.payChannelClientRouter = payChannelClientRouter;
        this.alipaySandboxProperties = alipaySandboxProperties;
    }

    public PayOrderView createPayOrder(String orderNo) {
        return createPayOrder(orderNo, PayOrder.CHANNEL_MOCK);
    }

    public PayOrderView createPayOrder(String orderNo, String payChannel) {
        return createPayOrder(orderNo, payChannel, "/orders");
    }

    public PayOrderView createPayOrder(String orderNo, String payChannel, String returnPath) {
        Order order = orderFacade.getByOrderNo(orderNo);
        String resolvedChannel = resolvePayChannel(payChannel);
        PayOrder existing = findReusablePayOrder(orderNo, resolvedChannel);
        if (existing != null) {
            payOrderDomainService.ensureExistingPayOrderUsable(existing, order);
            log.info("Reusing existing pay order, orderNo={}, payOrderNo={}, payChannel={}, payStatus={}",
                    orderNo, existing.payOrderNo(), existing.payChannel(), existing.payStatus());
            PayChannelSubmitResult submitResult = buildSubmitResult(existing, returnPath);
            log.info("Generated pay submit payload for reused order, orderNo={}, payOrderNo={}, payChannel={}, hasRedirectForm={}, redirectUrl={}",
                    orderNo, existing.payOrderNo(), existing.payChannel(), submitResult.redirectForm() != null && !submitResult.redirectForm().isBlank(), submitResult.redirectUrl());
            PayOrder resubmittedOrder = existing.withSubmission(submitResult.displayPayload());
            payOrderDomainService.update(resubmittedOrder);
            return payViewAssembler.toView(payOrderDomainService.loadByPayOrderNo(existing.payOrderNo()), submitResult);
        }
        closeOtherPendingPayOrders(orderNo, resolvedChannel);
        PayOrder payOrder = payOrderDomainService.save(payOrderDomainService.createPending(newPayOrderNo(), order, resolvedChannel));
        PayChannelSubmitResult submitResult = buildSubmitResult(payOrder, returnPath);
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
        if (payOrder.success()) {
            return payViewAssembler.toView(payOrder);
        }
        orderFacade.markPaid(orderNo);
        PayOrder successOrder = payOrder.markSuccess("MOCK-" + orderNo, LocalDateTime.now());
        payOrderDomainService.update(successOrder);
        return payViewAssembler.toView(payOrderDomainService.loadByPayOrderNo(successOrder.payOrderNo()));
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
                params.get("trade_status")
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
        PayCallbackRecord callbackRecord = payCallbackRecordRepository.save(PayCallbackRecord.create(
                request.channelCode(),
                request.payOrderNo(),
                request.orderNo(),
                request.outTradeNo(),
                request.transactionNo(),
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
            payCallbackRecordRepository.update(callbackRecord.markFailed());
            return payViewAssembler.toView(payOrder);
        }
        if (payOrder.success()) {
            log.info("Ignoring duplicated successful pay callback, orderNo={}, payOrderNo={}, tradeNo={}",
                    orderNo, payOrder.payOrderNo(), request.transactionNo());
            payCallbackRecordRepository.update(callbackRecord.markIgnored());
            return payViewAssembler.toView(payOrder);
        }
        if (request.tradeStatus() != null && !request.tradeStatus().isBlank()
                && !"TRADE_SUCCESS".equalsIgnoreCase(request.tradeStatus())
                && !"TRADE_FINISHED".equalsIgnoreCase(request.tradeStatus())) {
            log.info("Ignoring pay callback because trade status is not success, orderNo={}, payOrderNo={}, tradeNo={}, tradeStatus={}",
                    orderNo, payOrder.payOrderNo(), request.transactionNo(), request.tradeStatus());
            payCallbackRecordRepository.update(callbackRecord.markIgnored());
            return payViewAssembler.toView(payOrder);
        }
        Order order = orderFacade.getByOrderNo(orderNo);
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
        Order order = orderFacade.getByOrderNo(orderNo);
        PayOrder payOrder = payOrderRepository.findByOrderNo(orderNo).orElse(null);
        boolean consistent = payOrder == null || (payOrderDomainService.reconcileAmount(order, payOrder) && payOrderDomainService.reconcileStatus(order, payOrder));
        return payViewAssembler.toReconcileResult(orderNo, order, payOrder, consistent);
    }

    public PayOrderView repairPaidOrder(String orderNo) {
        Order order = orderFacade.getByOrderNo(orderNo);
        PayOrder payOrder = requireLatestPayOrder(orderNo);
        if (order.paidOrAfter() && payOrder.success()) {
            return payViewAssembler.toView(payOrder);
        }
        if (!payOrder.success()) {
            throw BusinessException.badRequest("当前支付单尚未成功，无法补偿订单支付状态");
        }
        if (!order.pendingPayment()) {
            throw BusinessException.badRequest("当前订单状态不允许补偿为已支付");
        }
        orderFacade.markPaid(orderNo);
        log.warn("Repaired paid order from successful pay order, orderNo={}, payOrderNo={}, payChannel={}",
                orderNo, payOrder.payOrderNo(), payOrder.payChannel());
        return payViewAssembler.toView(payOrder);
    }

    public PayOrderView syncOrderStatus(String orderNo) {
        Order order = orderFacade.getByOrderNo(orderNo);
        PayOrder payOrder = requireLatestPayOrder(orderNo);
        if (!payOrder.success() && isAlipayChannel(payOrder.payChannel())) {
            PayChannelQueryResult queryResult = payChannelClientRouter.route(payOrder.payChannel()).query(payOrder);
            if (queryResult.paid()) {
                payOrder = payOrder.markSuccess(queryResult.transactionNo(), LocalDateTime.now());
                payOrderDomainService.update(payOrder);
                log.warn("Marked pay order success from Alipay query, orderNo={}, payOrderNo={}, payChannel={}, tradeNo={}, tradeStatus={}",
                        orderNo, payOrder.payOrderNo(), payOrder.payChannel(), queryResult.transactionNo(), queryResult.tradeStatus());
            }
        }
        if (payOrder.success() && order.pendingPayment()) {
            orderFacade.markPaid(orderNo);
            log.warn("Synced order status from successful pay order, orderNo={}, payOrderNo={}, payChannel={}",
                    orderNo, payOrder.payOrderNo(), payOrder.payChannel());
        }
        return payViewAssembler.toView(payOrderDomainService.loadByPayOrderNo(payOrder.payOrderNo()));
    }

    private boolean isAlipayChannel(String payChannel) {
        return PayOrder.CHANNEL_ALIPAY_WAP.equals(payChannel) || PayOrder.CHANNEL_ALIPAY_PC.equals(payChannel);
    }

    public PayOrderView mockRefund(String orderNo, String reason) {
        PayOrder payOrder = payOrderDomainService.loadByOrderNo(orderNo);
        if (payOrder.refunded()) {
            return payViewAssembler.toView(payOrder);
        }
        PayOrder refundPending = payOrder.markRefundPending(LocalDateTime.now(), reason);
        payOrderDomainService.update(refundPending);
        PayOrder refunded = refundPending.markRefundSuccess(LocalDateTime.now(), reason);
        payOrderDomainService.update(refunded);
        return payViewAssembler.toView(payOrderDomainService.loadByOrderNo(orderNo));
    }

    public PayOrderView closePayOrder(String orderNo, String reason) {
        PayOrder payOrder = payOrderDomainService.loadByOrderNo(orderNo);
        payOrderDomainService.update(payOrder.close(reason));
        return payViewAssembler.toView(payOrderDomainService.loadByOrderNo(orderNo));
    }

    public void closePendingPayOrders(String orderNo, String reasonStatus) {
        List<PayOrder> pendingPayOrders = payOrderDomainService.loadPendingByOrderNo(orderNo);
        for (PayOrder payOrder : pendingPayOrders) {
            payOrderDomainService.update(payOrder.closeByOrderStatus(reasonStatus));
        }
    }

    public PayOrderView detail(String payOrderNo) {
        return payViewAssembler.toView(payOrderDomainService.loadByPayOrderNo(payOrderNo));
    }

    public String renderPaySubmitPage(String payOrderNo, String returnPath) {
        PayOrder payOrder = payOrderDomainService.loadByPayOrderNo(payOrderNo);
        if (!payOrder.pending()) {
            throw BusinessException.badRequest("当前支付单状态不允许继续支付");
        }
        PayChannelSubmitResult submitResult = buildSubmitResult(payOrder, returnPath);
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
        if (PayOrder.CHANNEL_ALIPAY_WAP.equals(payOrder.payChannel())) {
            String resolvedReturnUrl = buildReturnUrl(payOrder, returnPath);
            return payChannelClientRouter.route(payOrder.payChannel()).submit(payOrder, resolvedReturnUrl);
        }
        return payChannelClientRouter.route(payOrder.payChannel()).submit(payOrder);
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

    private String newPayOrderNo() {
        return "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
