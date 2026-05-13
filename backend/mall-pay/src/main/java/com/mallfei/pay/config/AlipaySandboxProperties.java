package com.mallfei.pay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mall.pay.alipay")
public class AlipaySandboxProperties {

    private boolean enabled = false;
    private String gateway = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private String appId = "";
    private String merchantPrivateKey = "";
    private String alipayPublicKey = "";
    private String notifyUrl = "http://localhost:9090/api/pay/callback/alipay";
    private String returnUrl = "http://localhost:9090/api/pay/alipay/return-bridge";
    private String clientReturnUrl = "http://localhost:5173/pay/return";
    private String signType = "RSA2";
    private String charset = "UTF-8";
    private String format = "json";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getMerchantPrivateKey() { return merchantPrivateKey; }
    public void setMerchantPrivateKey(String merchantPrivateKey) { this.merchantPrivateKey = merchantPrivateKey; }
    public String getAlipayPublicKey() { return alipayPublicKey; }
    public void setAlipayPublicKey(String alipayPublicKey) { this.alipayPublicKey = alipayPublicKey; }
    public String getNotifyUrl() { return notifyUrl; }
    public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    public String getClientReturnUrl() { return clientReturnUrl; }
    public void setClientReturnUrl(String clientReturnUrl) { this.clientReturnUrl = clientReturnUrl; }
    public String getSignType() { return signType; }
    public void setSignType(String signType) { this.signType = signType; }
    public String getCharset() { return charset; }
    public void setCharset(String charset) { this.charset = charset; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}
