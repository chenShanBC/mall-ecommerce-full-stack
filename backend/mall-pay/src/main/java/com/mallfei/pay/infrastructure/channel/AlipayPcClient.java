package com.mallfei.pay.infrastructure.channel;

import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.pay.config.AlipaySandboxProperties;
import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.pay.domain.service.PayChannelSubmitResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AlipayPcClient extends AlipayClient {

    public AlipayPcClient(AlipaySandboxProperties properties) {
        super(properties);
    }

    @Override
    public String channelCode() {
        return PayOrder.CHANNEL_ALIPAY_PC;
    }

    @Override
    protected String alipayMethod() {
        return "alipay.trade.page.pay";
    }

    @Override
    protected String productCode() {
        return "FAST_INSTANT_TRADE_PAY";
    }

    @Override
    public PayChannelSubmitResult submit(PayOrder payOrder, String returnUrl) {
        ensureConfigured();
        try {
            String bizContent = buildBizContent(payOrder);
            Map<String, String> signParams = buildSignParams(payOrder, bizContent, returnUrl);
            String signContent = buildSignContent(signParams);

            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(properties.getNotifyUrl());
            request.setReturnUrl(resolveReturnUrl(returnUrl));
            request.setBizModel(buildPageBizModel(payOrder));

            AlipayTradePagePayResponse response = createSdkClient().pageExecute(request, "GET");
            log.info("Generated Alipay PC pay page, payOrderNo={}, orderNo={}, returnUrl={}, responseCode={}, responseSubCode={}, responseMsg={}, responseSubMsg={}, bodyLength={}",
                    payOrder.payOrderNo(), payOrder.orderNo(), request.getReturnUrl(), response == null ? null : response.getCode(),
                    response == null ? null : response.getSubCode(), response == null ? null : response.getMsg(),
                    response == null ? null : response.getSubMsg(), response == null || response.getBody() == null ? 0 : response.getBody().length());

            if (response == null || !response.isSuccess() || response.getBody() == null || response.getBody().isBlank()) {
                throw BusinessException.badRequest("支付宝 PC 支付页生成失败，请检查沙箱配置和请求参数");
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
                    "",
                    response.getBody()
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Failed to generate Alipay PC pay page, payOrderNo={}, orderNo={}, returnUrl={}, message={}",
                    payOrder.payOrderNo(), payOrder.orderNo(), returnUrl, exception.getMessage(), exception);
            throw BusinessException.badRequest("支付宝 PC 支付页生成失败: " + exception.getMessage());
        }
    }

    private AlipayTradePagePayModel buildPageBizModel(PayOrder payOrder) {
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(payOrder.payOrderNo());
        model.setTotalAmount(toYuan(payOrder.payAmountCent()));
        model.setSubject(buildSubject(payOrder));
        model.setProductCode(productCode());
        model.setPassbackParams(payOrder.orderNo());
        return model;
    }
}
