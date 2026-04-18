package com.mallfei.auth.controller;

import com.mallfei.auth.application.dto.LoginRequest;
import com.mallfei.auth.application.service.AuthApplicationService;
import com.mallfei.common.api.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/login/password")
    public R<?> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authApplicationService.login(request));
    }

    @PostMapping("/register")
    public R<?> register(@RequestBody Map<String, Object> request) {
        return R.ok("注册成功，当前为演示注册逻辑", request);
    }

    @GetMapping("/me")
    public R<?> me() {
        return R.ok(authApplicationService.currentUser());
    }
}
