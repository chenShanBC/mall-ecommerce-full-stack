package com.mallfei.cart.controller;

import com.mallfei.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @GetMapping("/list")
    public R<?> list() {
        return R.ok(List.of(
                Map.of(
                        "id", 1,
                        "skuId", 1,
                        "skuName", "MVP 示例 SKU",
                        "count", 1,
                        "checked", true
                )
        ));
    }
}
