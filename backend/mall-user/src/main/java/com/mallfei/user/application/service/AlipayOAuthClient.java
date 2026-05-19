package com.mallfei.user.application.service;

public interface AlipayOAuthClient {

    AlipayOAuthTokenResult exchangeToken(String authCode);

    AlipayOAuthUserInfo fetchUserInfo(String accessToken);

    record AlipayOAuthTokenResult(String accessToken, String alipayUserId) {}

    record AlipayOAuthUserInfo(String userId, String nickName, String avatar) {}
}
