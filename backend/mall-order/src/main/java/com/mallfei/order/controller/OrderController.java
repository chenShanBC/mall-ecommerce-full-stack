package com.mallfei.order.controller;

import com.mallfei.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @GetMapping("/list")
    public R<?> list() {
        return R.ok(List.of(
                Map.of(
                        "orderNo", "MVP202604180001",
                        "status", "UNPAID",
                        "payAmount", 99.90
                )
        ));
    }
}
