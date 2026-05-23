package com.mallfei.user.application.service;

import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.common.enums.IdentityType;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.user.application.dto.AlipayAuthUrlView;
import com.mallfei.user.application.vo.UserLoginResult;
import com.mallfei.user.config.UserAlipayLoginProperties;
import com.mallfei.user.domain.model.UserAccount;
import com.mallfei.user.domain.model.UserThirdBind;
import com.mallfei.user.domain.repository.UserThirdBindRepository;
import com.mallfei.user.domain.service.AlipayLoginStateRepository;
import com.mallfei.user.domain.service.UserDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class AlipayLoginApplicationService {

    private final UserAlipayLoginProperties loginProps;
    private final com.mallfei.pay.config.AlipaySandboxProperties payProps;
    private final AlipayLoginStateRepository stateRepository;
    private final AlipayOAuthClient alipayOAuthClient;
    private final UserThirdBindRepository thirdBindRepository;
    private final UserDomainService userDomainService;
    private final AuthFacade authFacade;

    public AlipayLoginApplicationService(UserAlipayLoginProperties loginProps,
                                         com.mallfei.pay.config.AlipaySandboxProperties payProps,
                                         AlipayLoginStateRepository stateRepository,
                                         AlipayOAuthClient alipayOAuthClient,
                                         UserThirdBindRepository thirdBindRepository,
                                         UserDomainService userDomainService,
                                         AuthFacade authFacade) {
        this.loginProps = loginProps;
        this.payProps = payProps;
        this.stateRepository = stateRepository;
        this.alipayOAuthClient = alipayOAuthClient;
        this.thirdBindRepository = thirdBindRepository;
        this.userDomainService = userDomainService;
        this.authFacade = authFacade;
    }

    public AlipayAuthUrlView createAuthUrl() {
        String state = UUID.randomUUID().toString().replace("-", "");
        stateRepository.saveState(state, loginProps.getStateExpireSeconds());
        String authBase = resolveAuthBaseUrl();
        String authUrl = authBase
                + "?app_id=" + payProps.getAppId()
                + "&scope=auth_user"
                + "&redirect_uri=" + URLEncoder.encode(loginProps.getRedirectUri(), StandardCharsets.UTF_8)
                + "&state=" + state;
        return new AlipayAuthUrlView(authUrl, state, loginProps.getStateExpireSeconds());
    }

    private String resolveAuthBaseUrl() {
        String gateway = payProps.getGateway() == null ? "" : payProps.getGateway().toLowerCase();
        if (gateway.contains("sandbox") || gateway.contains("alipaydev")) {
            return "https://openauth-sandbox.dl.alipaydev.com/oauth2/publicAppAuthorize.htm";
        }
        return "https://openauth.alipay.com/oauth2/publicAppAuthorize.htm";
    }

    @Transactional
    public String handleCallback(String authCode, String state) {
        if (!stateRepository.consumeState(state)) {
            throw BusinessException.badRequest("授权状态已失效，请重新发起登录");
        }
        AlipayOAuthClient.AlipayOAuthTokenResult tokenResult = alipayOAuthClient.exchangeToken(authCode);
        AlipayOAuthClient.AlipayOAuthUserInfo userInfo = alipayOAuthClient.fetchUserInfo(tokenResult.accessToken());
        String alipayUserId = userInfo.userId() == null || userInfo.userId().isBlank() ? tokenResult.alipayUserId() : userInfo.userId();
        if (alipayUserId == null || alipayUserId.isBlank()) {
            throw BusinessException.badRequest("支付宝用户标识为空，无法登录");
        }

        Long userId = thirdBindRepository.findByThirdTypeAndUid(UserThirdBind.THIRD_TYPE_ALIPAY, alipayUserId)
                .map(UserThirdBind::userId)
                .orElseGet(() -> registerAndBind(alipayUserId, userInfo));

        String loginTicket = UUID.randomUUID().toString().replace("-", "");
        stateRepository.saveLoginTicket(loginTicket, userId, loginProps.getLoginTicketExpireSeconds());

        return UriComponentsBuilder.fromUriString(loginProps.getFrontendRedirectUri())
                .queryParam("loginTicket", loginTicket)
                .build(true)
                .toUriString();
    }

    public UserLoginResult exchangeLoginTicket(String loginTicket) {
        Long userId = stateRepository.consumeLoginTicket(loginTicket);
        if (userId == null) {
            throw BusinessException.badRequest("登录票据无效或已过期");
        }
        return buildLoginResult(userId);
    }

    @Transactional
    public UserLoginResult loginByJsapiAuthCode(String authCode) {
        if (authCode == null || authCode.isBlank()) {
            throw BusinessException.badRequest("授权码不能为空");
        }
        AlipayOAuthClient.AlipayOAuthTokenResult tokenResult = alipayOAuthClient.exchangeToken(authCode);
        AlipayOAuthClient.AlipayOAuthUserInfo userInfo = alipayOAuthClient.fetchUserInfo(tokenResult.accessToken());
        String alipayUserId = userInfo.userId() == null || userInfo.userId().isBlank() ? tokenResult.alipayUserId() : userInfo.userId();
        if (alipayUserId == null || alipayUserId.isBlank()) {
            throw BusinessException.badRequest("支付宝用户标识为空，无法登录");
        }
        Long userId = thirdBindRepository.findByThirdTypeAndUid(UserThirdBind.THIRD_TYPE_ALIPAY, alipayUserId)
                .map(UserThirdBind::userId)
                .orElseGet(() -> registerAndBind(alipayUserId, userInfo));
        return buildLoginResult(userId);
    }

    private UserLoginResult buildLoginResult(Long userId) {
        UserAccount userAccount = userDomainService.loadById(userId);
        ensureEnabled(userAccount);
        String token = authFacade.createLoginSession(
                userAccount.id(), userAccount.mobile(), IdentityType.USER,
                userAccount.nickname(), userAccount.avatarUrl(), "USER", List.of());
        return new UserLoginResult(token, userAccount.id(), userAccount.mobile(), userAccount.nickname(), userAccount.avatarUrl(), userAccount.mobile() != null && !userAccount.mobile().isBlank());
    }

    private void ensureEnabled(UserAccount userAccount) {
        if (authFacade.isUserDisabled(userAccount.id()) || !userAccount.enabled()) {
            authFacade.disableUserSession(userAccount.id());
            throw new com.mallfei.common.exception.BusinessException(UserDomainService.USER_DISABLED_CODE, UserDomainService.USER_DISABLED_MESSAGE);
        }
    }

    private Long registerAndBind(String alipayUserId, AlipayOAuthClient.AlipayOAuthUserInfo userInfo) {
        String nickname = userInfo.nickName() == null || userInfo.nickName().isBlank() ? "支付宝用户" : userInfo.nickName();
        UserAccount created = userDomainService.registerByThirdParty(nickname, userInfo.avatar(), UUID.randomUUID().toString().substring(0, 12));
        thirdBindRepository.save(new UserThirdBind(null, created.id(), UserThirdBind.THIRD_TYPE_ALIPAY, alipayUserId, userInfo.nickName(), userInfo.avatar()));
        return created.id();
    }
}
