package com.mallfei.pay.infrastructure.channel;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayConfig;
import com.alipay.api.AlipayConstants;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeFastpayRefundQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.pay.config.AlipaySandboxProperties;
import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.pay.domain.service.PayChannelCallbackRequest;
import com.mallfei.pay.domain.service.PayChannelQueryResult;
import com.mallfei.pay.domain.service.PayChannelSubmitResult;
import com.mallfei.pay.domain.service.PayRefundQueryRequest;
import com.mallfei.pay.domain.service.PayRefundQueryResult;
import com.mallfei.pay.domain.service.PayRefundRequest;
import com.mallfei.pay.domain.service.PayRefundResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class AlipayClient implements com.mallfei.pay.domain.service.PayChannelClient {

    protected static final Logger log = LoggerFactory.getLogger(AlipayClient.class);
    private static final DateTimeFormatter ALIPAY_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    protected final AlipaySandboxProperties properties;
    protected final long orderTimeoutMinutes;
    protected Duration remainingPayTime;

    public AlipayClient(AlipaySandboxProperties properties,
                        @Value("${mall.order.timeout-minutes:2}") long orderTimeoutMinutes) {
        this.properties = properties;
        this.orderTimeoutMinutes = orderTimeoutMinutes;
    }

    @Override
    public String channelCode() {
        return PayOrder.CHANNEL_ALIPAY_WAP;
    }

    @Override
    public PayChannelSubmitResult submit(PayOrder payOrder) {
        return submit(payOrder, properties.getReturnUrl());
    }

    @Override
    public PayChannelSubmitResult submit(PayOrder payOrder, String returnUrl) {
        return submit(payOrder, returnUrl, null);
    }

    @Override
    public PayChannelSubmitResult submit(PayOrder payOrder, String returnUrl, Duration remainingPayTime) {
        ensureConfigured();
        this.remainingPayTime = remainingPayTime;
        try {
            String bizContent = buildBizContent(payOrder);
            Map<String, String> signParams = buildSignParams(payOrder, bizContent, returnUrl);
            String signContent = buildSignContent(signParams);

            AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
            request.setNotifyUrl(properties.getNotifyUrl());
            request.setReturnUrl(resolveReturnUrl(returnUrl));
            request.setBizModel(buildWapBizModel(payOrder));

            AlipayTradeWapPayResponse response = createSdkClient().pageExecute(request);
            log.info("Generated Alipay WAP pay page, payOrderNo={}, orderNo={}, returnUrl={}, responseCode={}, responseSubCode={}, responseMsg={}, responseSubMsg={}, bodyLength={}",
                    payOrder.payOrderNo(), payOrder.orderNo(), request.getReturnUrl(), response == null ? null : response.getCode(),
                    response == null ? null : response.getSubCode(), response == null ? null : response.getMsg(),
                    response == null ? null : response.getSubMsg(), response == null || response.getBody() == null ? 0 : response.getBody().length());

            return buildSubmitResult(payOrder, returnUrl, signContent, bizContent, response);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Failed to generate Alipay WAP pay page, payOrderNo={}, orderNo={}, returnUrl={}, message={}",
                    payOrder.payOrderNo(), payOrder.orderNo(), returnUrl, exception.getMessage(), exception);
            throw BusinessException.badRequest("支付宝 H5 支付页生成失败，请稍后重试或切换模拟支付");
        }
    }

    @Override
    public boolean verifyCallback(PayChannelCallbackRequest request) {
        if (!properties.isEnabled()) {
            return false;
        }
        if (blank(request.signature()) || blank(properties.getAlipayPublicKey()) || blank(request.rawPayload())) {
            return false;
        }
        return AlipayRsa2Support.verify(request.rawPayload(), request.signature(), properties.getAlipayPublicKey(), charset());
    }

    @Override
    public PayChannelQueryResult query(PayOrder payOrder) {
        ensureConfigured();
        try {
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(payOrder.payOrderNo());

            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizModel(model);

            AlipayTradeQueryResponse response = createSdkClient().execute(request);
            String tradeStatus = response == null ? "" : response.getTradeStatus();
            String tradeNo = response == null ? "" : response.getTradeNo();
            String body = response == null ? "" : response.getBody();
            boolean paid = response != null
                    && response.isSuccess()
                    && ("TRADE_SUCCESS".equalsIgnoreCase(tradeStatus) || "TRADE_FINISHED".equalsIgnoreCase(tradeStatus));
            log.info("Queried Alipay trade, payOrderNo={}, orderNo={}, responseCode={}, subCode={}, tradeStatus={}, tradeNo={}, paid={}",
                    payOrder.payOrderNo(), payOrder.orderNo(), response == null ? null : response.getCode(),
                    response == null ? null : response.getSubCode(), tradeStatus, tradeNo, paid);
            return paid
                    ? PayChannelQueryResult.paid(tradeNo, tradeStatus, body)
                    : PayChannelQueryResult.unpaid(tradeStatus, body);
        } catch (Exception exception) {
            log.warn("Failed to query Alipay trade, payOrderNo={}, orderNo={}, message={}",
                    payOrder.payOrderNo(), payOrder.orderNo(), exception.getMessage());
            return PayChannelQueryResult.unpaid(payOrder.payStatus(), "");
        }
    }

    @Override
    public PayRefundResult refund(PayRefundRequest refundRequest) {
        ensureConfigured();
        try {
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();
            model.setOutTradeNo(refundRequest.payOrder().payOrderNo());
            model.setRefundAmount(toYuan(refundRequest.refundAmountCent()));
            model.setRefundReason(refundRequest.reason());
            model.setOutRequestNo(refundRequest.refundNo());

            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            request.setBizModel(model);

            AlipayTradeRefundResponse response = createSdkClient().execute(request);
            String body = response == null ? "" : response.getBody();
            boolean success = response != null && response.isSuccess();
            log.info("Requested Alipay refund, payOrderNo={}, orderNo={}, refundNo={}, amountCent={}, responseCode={}, subCode={}, success={}",
                    refundRequest.payOrder().payOrderNo(), refundRequest.payOrder().orderNo(), refundRequest.refundNo(), refundRequest.refundAmountCent(),
                    response == null ? null : response.getCode(), response == null ? null : response.getSubCode(), success);
            if (success) {
                return PayRefundResult.success(refundRequest.refundNo(), response.getTradeNo(), body);
            }
            return PayRefundResult.failed(response == null ? "ALIPAY_REFUND_FAILED" : response.getSubCode(),
                    response == null ? "支付宝退款失败" : response.getSubMsg(), body);
        } catch (Exception exception) {
            log.error("Failed to request Alipay refund, payOrderNo={}, orderNo={}, refundNo={}, message={}",
                    refundRequest.payOrder().payOrderNo(), refundRequest.payOrder().orderNo(), refundRequest.refundNo(), exception.getMessage(), exception);
            return PayRefundResult.failed("ALIPAY_REFUND_EXCEPTION", exception.getMessage(), "");
        }
    }

    @Override
    public PayRefundQueryResult queryRefund(PayRefundQueryRequest refundQueryRequest) {
        ensureConfigured();
        try {
            AlipayTradeFastpayRefundQueryModel model = new AlipayTradeFastpayRefundQueryModel();
            model.setOutTradeNo(refundQueryRequest.payOrder().payOrderNo());
            model.setOutRequestNo(refundQueryRequest.refundNo());

            AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
            request.setBizModel(model);

            AlipayTradeFastpayRefundQueryResponse response = createSdkClient().execute(request);
            String body = response == null ? "" : response.getBody();
            boolean success = response != null && response.isSuccess();
            log.info("Queried Alipay refund, payOrderNo={}, orderNo={}, refundNo={}, responseCode={}, subCode={}, success={}",
                    refundQueryRequest.payOrder().payOrderNo(), refundQueryRequest.payOrder().orderNo(), refundQueryRequest.refundNo(),
                    response == null ? null : response.getCode(), response == null ? null : response.getSubCode(), success);
            if (success) {
                return PayRefundQueryResult.success(refundQueryRequest.refundNo(), response.getTradeNo(), "REFUND_SUCCESS", body);
            }
            return PayRefundQueryResult.unknown(refundQueryRequest.refundNo(),
                    response == null ? "UNKNOWN" : response.getSubCode(),
                    response == null ? "支付宝退款查询无响应" : response.getSubMsg(), body);
        } catch (Exception exception) {
            log.warn("Failed to query Alipay refund, payOrderNo={}, orderNo={}, refundNo={}, message={}",
                    refundQueryRequest.payOrder().payOrderNo(), refundQueryRequest.payOrder().orderNo(), refundQueryRequest.refundNo(), exception.getMessage());
            return PayRefundQueryResult.unknown(refundQueryRequest.refundNo(), "QUERY_EXCEPTION", exception.getMessage(), "");
        }
    }

    protected String alipayMethod() {
        return "alipay.trade.wap.pay";
    }

    protected String productCode() {
        return "QUICK_WAP_WAY";
    }

    protected AlipayTradeWapPayModel buildWapBizModel(PayOrder payOrder) {
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(payOrder.payOrderNo());
        model.setTotalAmount(toYuan(payOrder.payAmountCent()));
        model.setSubject(buildSubject(payOrder));
        model.setProductCode(productCode());
        model.setTimeoutExpress(alipayTimeoutExpress());
        model.setQuitUrl(properties.getClientReturnUrl());
        model.setPassbackParams(payOrder.orderNo());
        return model;
    }

    protected com.alipay.api.AlipayClient createSdkClient() throws AlipayApiException {
        AlipayConfig config = new AlipayConfig();
        config.setServerUrl(properties.getGateway());
        config.setAppId(properties.getAppId());
        config.setPrivateKey(properties.getMerchantPrivateKey());
        config.setFormat(properties.getFormat());
        config.setCharset(properties.getCharset());
        config.setSignType(properties.getSignType());
        config.setAlipayPublicKey(properties.getAlipayPublicKey());
        return new DefaultAlipayClient(config);
    }

    protected Map<String, String> buildSignParams(PayOrder payOrder, String bizContent, String returnUrl) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", properties.getAppId());
        params.put("method", alipayMethod());
        params.put("format", properties.getFormat());
        params.put("charset", properties.getCharset());
        params.put("sign_type", properties.getSignType());
        params.put("notify_url", properties.getNotifyUrl());
        params.put("return_url", resolveReturnUrl(returnUrl));
        params.put("timestamp", LocalDateTime.now().format(ALIPAY_TIMESTAMP_FORMATTER));
        params.put("version", AlipayConstants.VERSION);
        params.put("biz_content", bizContent);
        return params;
    }

    protected String buildBizContent(PayOrder payOrder) {
        return "{\"out_trade_no\":\"" + jsonEscape(payOrder.payOrderNo())
                + "\",\"total_amount\":\"" + toYuan(payOrder.payAmountCent())
                + "\",\"subject\":\"" + jsonEscape(buildSubject(payOrder))
                + "\",\"product_code\":\"" + productCode()
                + "\",\"timeout_express\":\"" + jsonEscape(alipayTimeoutExpress())
                + "\",\"passback_params\":\"" + jsonEscape(payOrder.orderNo()) + "\"}";
    }

    protected String buildSubject(PayOrder payOrder) {
        return "mallFei订单-" + payOrder.orderNo();
    }

    protected String alipayTimeoutExpress() {
        if (remainingPayTime == null) {
            return Math.max(1, orderTimeoutMinutes) + "m";
        }
        long remainingSeconds = remainingPayTime.getSeconds();
        if (remainingSeconds < 90) {
            throw BusinessException.badRequest("订单即将超时，请重新下单");
        }
        long minutes = Math.max(1, remainingSeconds / 60);
        return minutes + "m";
    }

    protected void ensureConfigured() {
        if (!properties.isEnabled()) {
            throw BusinessException.badRequest("支付宝沙箱未启用，请先开启 mall.pay.alipay.enabled");
        }
        if (blank(properties.getAppId()) || blank(properties.getMerchantPrivateKey()) || blank(properties.getAlipayPublicKey())) {
            throw BusinessException.badRequest("支付宝沙箱配置不完整，请补齐 appId、merchantPrivateKey、alipayPublicKey");
        }
    }

    protected String buildSignContent(Map<String, String> params) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(entry -> !blank(entry.getValue()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    protected Charset charset() {
        return Charset.forName(properties.getCharset());
    }

    protected String toYuan(Long cent) {
        long value = cent == null ? 0L : cent;
        return String.format(Locale.ROOT, "%.2f", value / 100.0D);
    }

    protected boolean blank(String value) {
        return value == null || value.isBlank();
    }

    protected String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    protected String resolveReturnUrl(String returnUrl) {
        return returnUrl == null || returnUrl.isBlank() ? properties.getReturnUrl() : returnUrl;
    }

    protected PayChannelSubmitResult buildSubmitResult(PayOrder payOrder,
                                                       String returnUrl,
                                                       String signContent,
                                                       String bizContent,
                                                       AlipayTradeWapPayResponse response) {
        if (response == null || !response.isSuccess() || response.getBody() == null || response.getBody().isBlank()) {
            throw BusinessException.badRequest("支付宝支付页生成失败，请检查沙箱配置和请求参数");
        }

        String displayPayload = "{\"channel\":\"" + jsonEscape(channelCode())
                + "\",\"gateway\":\"" + jsonEscape(properties.getGateway())
                + "\",\"method\":\"" + jsonEscape(alipayMethod())
                + "\",\"appId\":\"" + jsonEscape(properties.getAppId())
                + "\",\"notifyUrl\":\"" + jsonEscape(properties.getNotifyUrl())
                + "\",\"returnUrl\":\"" + jsonEscape(resolveReturnUrl(returnUrl))
                + "\",\"signContent\":\"" + jsonEscape(signContent)
                + "\",\"bizContent\":" + bizContent + "}";

        return new PayChannelSubmitResult(
                channelCode(),
                displayPayload,
                response.getBody(),
                properties.getGateway()
        );
    }
}
