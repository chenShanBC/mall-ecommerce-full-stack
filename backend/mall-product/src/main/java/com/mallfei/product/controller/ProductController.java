package com.mallfei.product.controller;

import com.mallfei.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @GetMapping("/category/tree")
    public R<?> categoryTree() {
        return R.ok(List.of(
                Map.of("id", 1, "name", "精选推荐", "children", List.of()),
                Map.of("id", 2, "name", "手机数码", "children", List.of()),
                Map.of("id", 3, "name", "服饰箱包", "children", List.of())
        ));
    }

    @GetMapping("/spu/page")
    public R<?> page() {
        return R.ok(Map.of(
                "total", 1,
                "list", List.of(
                        Map.of(
                                "id", 1,
                                "name", "MVP 示例商品",
                                "mainImage", "https://via.placeholder.com/300x300.png?text=mallFei",
                                "salePrice", new BigDecimal("99.90"),
                                "originPrice", new BigDecimal("129.90"),
                                "sales", 10
                        )
                )
        ));
    }

    @GetMapping("/spu/{id}")
    public R<?> detail(@PathVariable Long id) {
        return R.ok(Map.of(
                "id", id,
                "name", "MVP 示例商品",
                "mainImage", "https://via.placeholder.com/300x300.png?text=mallFei",
                "description", "这里是商品详情占位数据",
                "salePrice", new BigDecimal("99.90"),
                "originPrice", new BigDecimal("129.90"),
                "stock", 100
        ));
    }
}
