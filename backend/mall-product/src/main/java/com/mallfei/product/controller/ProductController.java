package com.mallfei.product.controller;

import com.mallfei.common.api.ApiResponse;
import com.mallfei.common.api.PageResponse;
import com.mallfei.product.application.service.ProductQueryApplicationService;
import com.mallfei.product.application.vo.CategoryTreeNodeView;
import com.mallfei.product.application.vo.ProductCardView;
import com.mallfei.product.application.vo.ProductDetailView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "商品公共接口")
public class ProductController {

    private final ProductQueryApplicationService productQueryApplicationService;

    public ProductController(ProductQueryApplicationService productQueryApplicationService) {
        this.productQueryApplicationService = productQueryApplicationService;
    }

    @Operation(summary = "获取类目树")
    @GetMapping("/api/categories")
    public ApiResponse<List<CategoryTreeNodeView>> categoryTree() {
        return ApiResponse.success(productQueryApplicationService.categories());
    }

    @Operation(summary = "获取商品列表")
    @GetMapping("/api/products")
    public ApiResponse<PageResponse<ProductCardView>> page() {
        return ApiResponse.success(productQueryApplicationService.productPage());
    }

    @Operation(summary = "获取商品详情")
    @GetMapping("/api/products/{productId}")
    public ApiResponse<ProductDetailView> detail(@PathVariable Long productId) {
        return ApiResponse.success(productQueryApplicationService.productDetail(productId));
    }
}
