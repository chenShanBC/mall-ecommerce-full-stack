package com.mallfei.pay.controller;

import com.mallfei.common.api.R;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/pay")
public class PayController {

    @PostMapping("/mock-success")
    public R<?> mockSuccess() {
        return R.ok(Map.of("status", "SUCCESS", "message", "模拟支付成功"));
    }
}
