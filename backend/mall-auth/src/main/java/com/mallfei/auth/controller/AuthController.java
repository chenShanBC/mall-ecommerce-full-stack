package com.mallfei.auth.controller;

import com.mallfei.auth.application.dto.AuthContextView;
import com.mallfei.auth.application.service.AuthApplicationService;
import com.mallfei.common.api.ApiResponse;
import com.mallfei.common.auth.RequireLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证聚合")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @RequireLogin
    @Operation(summary = "获取当前登录上下文")
    @GetMapping("/context")
    public ApiResponse<?> context() {
        return ApiResponse.success(AuthContextView.from(authApplicationService.currentPrincipal()));
    }

    @Operation(summary = "校验当前用户是否在黑名单")
    @GetMapping("/blacklist/{userId}")
    public ApiResponse<?> blacklist(@PathVariable Long userId) {
        return ApiResponse.success(authApplicationService.isUserDisabled(userId));
    }

    @RequireLogin
    @Operation(summary = "获取当前主体信息")
    @GetMapping("/me")
    public ApiResponse<?> me() {
        return ApiResponse.success(authApplicationService.currentPrincipal());
    }

    @RequireLogin
    @Operation(summary = "聚合退出登录")
    @DeleteMapping("/logout")
    public ApiResponse<?> logout() {
        authApplicationService.logout();
        return ApiResponse.success("退出成功", Boolean.TRUE);
    }
}
