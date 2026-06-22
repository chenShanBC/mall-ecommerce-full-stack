package com.mallfei.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mall.user.alipay-login")
public class UserAlipayLoginProperties {

    private String gateway = "https://openapi.alipay.com/gateway.do";
    private String appId = "";
    private String merchantPrivateKey = "";
    private String alipayPublicKey = "";
    private String signType = "RSA2";
    private String charset = "UTF-8";
    private String format = "json";
    private String redirectUri = "https://your-local-tunnel-domain/api/users/login/alipay/callback";
    private String frontendRedirectUri = "https://your-local-tunnel-domain/login";
    private int stateExpireSeconds = 300;
    private int loginTicketExpireSeconds = 120;

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getMerchantPrivateKey() { return merchantPrivateKey; }
    public void setMerchantPrivateKey(String merchantPrivateKey) { this.merchantPrivateKey = merchantPrivateKey; }
    public String getAlipayPublicKey() { return alipayPublicKey; }
    public void setAlipayPublicKey(String alipayPublicKey) { this.alipayPublicKey = alipayPublicKey; }
    public String getSignType() { return signType; }
    public void setSignType(String signType) { this.signType = signType; }
    public String getCharset() { return charset; }
    public void setCharset(String charset) { this.charset = charset; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    public String getFrontendRedirectUri() { return frontendRedirectUri; }
    public void setFrontendRedirectUri(String frontendRedirectUri) { this.frontendRedirectUri = frontendRedirectUri; }
    public int getStateExpireSeconds() { return stateExpireSeconds; }
    public void setStateExpireSeconds(int stateExpireSeconds) { this.stateExpireSeconds = stateExpireSeconds; }
    public int getLoginTicketExpireSeconds() { return loginTicketExpireSeconds; }
    public void setLoginTicketExpireSeconds(int loginTicketExpireSeconds) { this.loginTicketExpireSeconds = loginTicketExpireSeconds; }
}
