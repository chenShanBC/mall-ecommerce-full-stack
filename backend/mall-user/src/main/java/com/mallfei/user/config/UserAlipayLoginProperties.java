package com.mallfei.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mall.user.alipay-login")
public class UserAlipayLoginProperties {

    private String redirectUri = "https://your-local-tunnel-domain/api/users/login/alipay/callback";
    private String frontendRedirectUri = "https://your-local-tunnel-domain/login";
    private int stateExpireSeconds = 300;
    private int loginTicketExpireSeconds = 120;

    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    public String getFrontendRedirectUri() { return frontendRedirectUri; }
    public void setFrontendRedirectUri(String frontendRedirectUri) { this.frontendRedirectUri = frontendRedirectUri; }
    public int getStateExpireSeconds() { return stateExpireSeconds; }
    public void setStateExpireSeconds(int stateExpireSeconds) { this.stateExpireSeconds = stateExpireSeconds; }
    public int getLoginTicketExpireSeconds() { return loginTicketExpireSeconds; }
    public void setLoginTicketExpireSeconds(int loginTicketExpireSeconds) { this.loginTicketExpireSeconds = loginTicketExpireSeconds; }
}
