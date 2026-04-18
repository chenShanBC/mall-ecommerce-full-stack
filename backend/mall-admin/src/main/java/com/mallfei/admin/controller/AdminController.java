package com.mallfei.admin.controller;

import com.mallfei.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @PostMapping("/auth/login")
    public R<?> login(@RequestBody Map<String, Object> request) {
        return R.ok(Map.of(
                "token", "admin-demo-token",
                "username", request.getOrDefault("username", "admin"),
                "nickname", "系统管理员"
        ));
    }

    @GetMapping("/dashboard")
    public R<?> dashboard() {
        return R.ok(Map.of(
                "productCount", 1,
                "userCount", 1,
                "orderCount", 1,
                "pendingOrderCount", 1
        ));
    }

    @GetMapping("/product/spu/page")
    public R<?> productPage() {
        return R.ok(Map.of(
                "total", 1,
                "list", List.of(
                        Map.of(
                                "id", 1,
                                "name", "MVP 示例商品",
                                "status", 1,
                                "salePrice", 99.90,
                                "stock", 100
                        )
                )
        ));
    }
}
