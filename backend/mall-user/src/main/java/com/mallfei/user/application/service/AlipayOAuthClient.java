package com.mallfei.user.application.service;

public interface AlipayOAuthClient {

    AlipayOAuthTokenResult exchangeToken(String authCode);

    AlipayOAuthUserInfo fetchUserInfo(String accessToken);

    record AlipayOAuthTokenResult(String accessToken, String alipayUserId) {}

    record AlipayOAuthUserInfo(String userId, String openId, String nickName, String avatar) {
        public String stableUserId() {
            return userId == null || userId.isBlank() ? openId : userId;
        }
    }
}
