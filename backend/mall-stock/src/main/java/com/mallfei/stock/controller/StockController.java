package com.mallfei.stock.controller;

import com.mallfei.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    @GetMapping("/health")
    public R<?> health() {
        return R.ok(Map.of("module", "stock", "status", "ready"));
    }
}
