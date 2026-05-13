package com.mallfei.cart.controller;

import com.mallfei.cart.application.dto.CartAddItemRequest;
import com.mallfei.cart.application.dto.CartCheckoutRequest;
import com.mallfei.cart.application.dto.CartPrepareCheckoutRequest;
import com.mallfei.cart.application.dto.CartSelectItemsRequest;
import com.mallfei.cart.application.dto.CartUpdateItemRequest;
import com.mallfei.cart.application.service.CartApplicationService;
import com.mallfei.cart.application.vo.CartCheckoutValidationView;
import com.mallfei.cart.application.vo.CartListView;
import com.mallfei.cart.application.vo.CartOperationResultView;
import com.mallfei.cart.application.vo.CartQuantityView;
import com.mallfei.cart.application.vo.CartSettlementPreviewView;
import com.mallfei.common.api.ApiResponse;
import com.mallfei.common.auth.RequireUser;
import com.mallfei.order.application.vo.OrderDetailView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequireUser
@Tag(name = "购物车")
public class CartController {

    private final CartApplicationService cartApplicationService;

    public CartController(CartApplicationService cartApplicationService) {
        this.cartApplicationService = cartApplicationService;
    }

    @Operation(summary = "加入购物车")
    @PostMapping("/items")
    public ApiResponse<CartOperationResultView> addItem(@Valid @RequestBody CartAddItemRequest request) {
        return ApiResponse.success(cartApplicationService.addItem(request));
    }

    @Operation(summary = "获取购物车商品列表")
    @GetMapping("/items")
    public ApiResponse<CartListView> list() {
        return ApiResponse.success(cartApplicationService.listCurrentUserItems());
    }

    @Operation(summary = "编辑购物车商品项")
    @PutMapping("/items/{cartItemId}")
    public ApiResponse<CartOperationResultView> updateItem(@PathVariable Long cartItemId,
                                                           @Valid @RequestBody CartUpdateItemRequest request) {
        return ApiResponse.success(cartApplicationService.updateItem(cartItemId, request));
    }

    @Operation(summary = "设置购物车商品项勾选状态")
    @PutMapping("/items/checked")
    public ApiResponse<CartOperationResultView> selectItems(@Valid @RequestBody CartSelectItemsRequest request) {
        return ApiResponse.success(cartApplicationService.selectItems(request));
    }

    @Operation(summary = "删除单个购物车商品项")
    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<CartOperationResultView> deleteItem(@PathVariable Long cartItemId) {
        return ApiResponse.success(cartApplicationService.deleteItem(cartItemId));
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping("/items")
    public ApiResponse<CartOperationResultView> clear() {
        return ApiResponse.success(cartApplicationService.clearCurrentUserCart());
    }

    @Operation(summary = "获取购物车数量")
    @GetMapping("/quantity")
    public ApiResponse<CartQuantityView> quantity() {
        return ApiResponse.success(cartApplicationService.currentQuantity());
    }

    @Operation(summary = "获取结算预览")
    @GetMapping("/settlement-preview")
    public ApiResponse<CartSettlementPreviewView> settlementPreview() {
        return ApiResponse.success(cartApplicationService.settlementPreview());
    }

    @Operation(summary = "结算前校验")
    @PostMapping("/prepare-checkout")
    public ApiResponse<CartCheckoutValidationView> prepareCheckout(@Valid @RequestBody CartPrepareCheckoutRequest request) {
        return ApiResponse.success(cartApplicationService.prepareCheckout(request));
    }

    @Operation(summary = "购物车结算下单")
    @PostMapping("/checkout")
    public ApiResponse<OrderDetailView> checkout(@Valid @RequestBody CartCheckoutRequest request) {
        return ApiResponse.success(cartApplicationService.checkout(request));
    }
}
