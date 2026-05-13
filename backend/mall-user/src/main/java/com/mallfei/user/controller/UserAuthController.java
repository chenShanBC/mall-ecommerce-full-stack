package com.mallfei.user.controller;

import com.mallfei.common.api.ApiResponse;
import com.mallfei.common.auth.RequireUser;
import com.mallfei.user.application.dto.LoginCaptchaVerifyRequest;
import com.mallfei.user.application.dto.UserPasswordChangeRequest;
import com.mallfei.user.application.dto.UserPasswordLoginRequest;
import com.mallfei.user.application.dto.UserProfileUpdateRequest;
import com.mallfei.user.application.dto.UserRegisterRequest;
import com.mallfei.user.application.dto.UserSmsCodeLoginRequest;
import com.mallfei.user.application.dto.UserSmsCodeSendRequest;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "用户认证")
public class UserAuthController {

    private final UserApplicationService userApplicationService;

    public UserAuthController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
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
}
