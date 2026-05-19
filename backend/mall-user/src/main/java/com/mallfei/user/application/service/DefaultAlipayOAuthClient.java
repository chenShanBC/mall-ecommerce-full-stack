package com.mallfei.user.application.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayUserInfoShareRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayUserInfoShareResponse;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.pay.config.AlipaySandboxProperties;
import org.springframework.stereotype.Component;

@Component
public class DefaultAlipayOAuthClient implements AlipayOAuthClient {

    private final AlipaySandboxProperties props;

    public DefaultAlipayOAuthClient(AlipaySandboxProperties props) {
        this.props = props;
    }

    @Override
    public AlipayOAuthTokenResult exchangeToken(String authCode) {
        try {
            AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
            request.setGrantType("authorization_code");
            request.setCode(authCode);
            AlipaySystemOauthTokenResponse resp = client().execute(request);
            if (resp == null || !resp.isSuccess()) {
                throw BusinessException.badRequest("支付宝授权码换取令牌失败");
            }
            return new AlipayOAuthTokenResult(resp.getAccessToken(), resp.getUserId());
        } catch (Exception e) {
            throw BusinessException.badRequest("支付宝授权登录失败，请重试");
        }
    }

    @Override
    public AlipayOAuthUserInfo fetchUserInfo(String accessToken) {
        try {
            AlipayUserInfoShareRequest request = new AlipayUserInfoShareRequest();
            AlipayUserInfoShareResponse resp = client().execute(request, accessToken);
            if (resp == null || !resp.isSuccess()) {
                throw BusinessException.badRequest("支付宝用户信息获取失败");
            }
            return new AlipayOAuthUserInfo(resp.getUserId(), resp.getNickName(), resp.getAvatar());
        } catch (Exception e) {
            throw BusinessException.badRequest("支付宝用户信息获取失败");
        }
    }

    private AlipayClient client() {
        return new DefaultAlipayClient(
                props.getGateway(),
                props.getAppId(),
                props.getMerchantPrivateKey(),
                props.getFormat(),
                props.getCharset(),
                props.getAlipayPublicKey(),
                props.getSignType()
        );
    }
}
