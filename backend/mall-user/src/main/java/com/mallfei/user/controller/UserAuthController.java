package com.mallfei.user.controller;

import com.mallfei.common.api.ApiResponse;
import com.mallfei.common.auth.RequireUser;
import com.mallfei.user.application.dto.AlipayJsapiLoginRequest;
import com.mallfei.user.application.dto.AlipayLoginExchangeRequest;
import com.mallfei.user.application.dto.LoginCaptchaVerifyRequest;
import com.mallfei.user.application.dto.UserMobileBindCodeSendRequest;
import com.mallfei.user.application.dto.UserMobileBindRequest;
import com.mallfei.user.application.dto.UserPasswordChangeRequest;
import com.mallfei.user.application.dto.UserPasswordLoginRequest;
import com.mallfei.user.application.dto.UserProfileUpdateRequest;
import com.mallfei.user.application.dto.UserRegisterRequest;
import com.mallfei.user.application.dto.UserSmsCodeLoginRequest;
import com.mallfei.user.application.dto.UserSmsCodeSendRequest;
import com.mallfei.user.application.service.AlipayLoginApplicationService;
import com.mallfei.user.application.service.UserApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "用户认证")
public class UserAuthController {

    private final UserApplicationService userApplicationService;
    private final AlipayLoginApplicationService alipayLoginApplicationService;

    public UserAuthController(UserApplicationService userApplicationService,
                              AlipayLoginApplicationService alipayLoginApplicationService) {
        this.userApplicationService = userApplicationService;
        this.alipayLoginApplicationService = alipayLoginApplicationService;
    }

    @Operation(summary = "检查手机号是否在禁用黑名单")
    @GetMapping("/login/blacklist/status")
    public ApiResponse<?> blacklistStatus(@RequestParam("mobile") String mobile) {
        return ApiResponse.success(userApplicationService.blacklistStatusByMobile(mobile));
    }

    @Operation(summary = "检查当前登录用户是否在禁用黑名单")
    @GetMapping("/me/blacklist-status")
    @RequireUser
    public ApiResponse<?> currentUserBlacklistStatus() {
        return ApiResponse.success(userApplicationService.blacklistStatusByMobile(userApplicationService.currentUser().mobile()));
    }

    @Operation(summary = "获取登录拼图验证码挑战")
    @GetMapping("/login/captcha/challenge")
    public ApiResponse<?> loginCaptchaChallenge() {
        return ApiResponse.success(userApplicationService.createLoginCaptchaChallenge());
    }

    @Operation(summary = "校验登录拼图验证码")
    @PostMapping("/login/captcha/verify")
    public ApiResponse<?> verifyLoginCaptcha(@Valid @RequestBody LoginCaptchaVerifyRequest request) {
        return ApiResponse.success(userApplicationService.verifyLoginCaptcha(request));
    }

    @Operation(summary = "用户密码登录")
    @PostMapping("/login/password")
    public ApiResponse<?> login(@Valid @RequestBody UserPasswordLoginRequest request) {
        return ApiResponse.success(userApplicationService.login(request));
    }

    @Operation(summary = "发送登录验证码")
    @PostMapping("/login/sms/send-code")
    public ApiResponse<?> sendLoginSmsCode(@Valid @RequestBody UserSmsCodeSendRequest request) {
        System.out.println("[SMS_API_ENTER] /api/users/login/sms/send-code mobile=" + request.mobile());
        return ApiResponse.success("验证码发送成功", userApplicationService.sendLoginSmsCode(request));
    }

    @Operation(summary = "用户验证码登录")
    @PostMapping("/login/sms")
    public ApiResponse<?> loginBySmsCode(@Valid @RequestBody UserSmsCodeLoginRequest request) {
        return ApiResponse.success(userApplicationService.loginBySmsCode(request));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody UserRegisterRequest request) {
        return ApiResponse.success("注册并登录成功", userApplicationService.register(request));
    }

    @Operation(summary = "获取支付宝授权地址")
    @GetMapping("/login/alipay/auth-url")
    public ApiResponse<?> alipayAuthUrl() {
        return ApiResponse.success(alipayLoginApplicationService.createAuthUrl());
    }

    @Operation(summary = "支付宝授权回调")
    @GetMapping("/login/alipay/callback")
    public org.springframework.web.servlet.view.RedirectView alipayCallback(@RequestParam("auth_code") String authCode,
                                                                            @RequestParam("state") String state) {
        return new org.springframework.web.servlet.view.RedirectView(alipayLoginApplicationService.handleCallback(authCode, state));
    }

    @Operation(summary = "支付宝登录票据换取登录态")
    @PostMapping("/login/alipay/exchange")
    public ApiResponse<?> alipayExchange(@Valid @RequestBody AlipayLoginExchangeRequest request) {
        return ApiResponse.success(alipayLoginApplicationService.exchangeLoginTicket(request.loginTicket()));
    }

    @Operation(summary = "支付宝内H5通过authCode直接登录")
    @PostMapping("/login/alipay/jsapi-exchange")
    public ApiResponse<?> alipayJsapiExchange(@Valid @RequestBody AlipayJsapiLoginRequest request) {
        return ApiResponse.success(alipayLoginApplicationService.loginByJsapiAuthCode(request.authCode()));
    }

    @RequireUser
    @Operation(summary = "用户退出登录")
    @DeleteMapping("/logout")
    public ApiResponse<?> logout() {
        userApplicationService.logout();
        return ApiResponse.success("退出成功", Boolean.TRUE);
    }

    @RequireUser
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public ApiResponse<?> me() {
        return ApiResponse.success(userApplicationService.currentUser());
    }

    @RequireUser
    @Operation(summary = "更新当前用户资料")
    @PutMapping("/me")
    public ApiResponse<?> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.success("资料更新成功", userApplicationService.updateCurrentUserProfile(request));
    }

    @RequireUser
    @Operation(summary = "修改当前用户密码")
    @PutMapping("/me/password")
    public ApiResponse<?> changePassword(@Valid @RequestBody UserPasswordChangeRequest request) {
        userApplicationService.changeCurrentUserPassword(request);
        return ApiResponse.success("密码修改成功，请重新登录", Boolean.TRUE);
    }

    @RequireUser
    @Operation(summary = "发送手机号绑定/换绑验证码")
    @PostMapping("/me/mobile/send-code")
    public ApiResponse<?> sendMobileBindCode(@Valid @RequestBody UserMobileBindCodeSendRequest request) {
        return ApiResponse.success("验证码发送成功", userApplicationService.sendMobileBindSmsCode(request));
    }

    @RequireUser
    @Operation(summary = "绑定/换绑当前用户手机号")
    @PutMapping("/me/mobile")
    public ApiResponse<?> bindMobile(@Valid @RequestBody UserMobileBindRequest request) {
        return ApiResponse.success("手机号绑定成功", userApplicationService.bindCurrentUserMobile(request));
    }
}
