package com.mallfei.order.controller;

import com.mallfei.common.api.ApiResponse;
import com.mallfei.common.auth.RequireUser;
import com.mallfei.order.application.dto.OrderCreateRequest;
import com.mallfei.order.application.dto.OrderRefundApplyRequest;
import com.mallfei.order.application.service.OrderApplicationService;
import com.mallfei.order.application.vo.OrderDetailView;
import com.mallfei.order.application.vo.OrderRefundView;
import com.mallfei.order.application.vo.OrderSummaryView;
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

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequireUser
@Tag(name = "订单")
public class OrderController {

    private final OrderApplicationService orderApplicationService;

    public OrderController(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @Operation(summary = "获取当前用户订单列表")
    @GetMapping
    public ApiResponse<List<OrderSummaryView>> list() {
        return ApiResponse.success(orderApplicationService.currentUserOrders());
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailView> detail(@PathVariable Long orderId) {
        return ApiResponse.success(orderApplicationService.currentUserOrder(orderId));
    }

    @Operation(summary = "创建订单")
    @PostMapping
    public ApiResponse<OrderDetailView> create(@Valid @RequestBody OrderCreateRequest request) {
        return ApiResponse.success(orderApplicationService.createOrder(request));
    }

    @Operation(summary = "取消订单")
    @DeleteMapping("/{orderId}/cancel")
    public ApiResponse<OrderDetailView> cancel(@PathVariable Long orderId) {
        return ApiResponse.success(orderApplicationService.cancelOrder(orderId));
    }

    @Operation(summary = "用户删除订单")
    @DeleteMapping("/{orderId}")
    public ApiResponse<Void> delete(@PathVariable Long orderId) {
        orderApplicationService.deleteCurrentUserOrder(orderId);
        return ApiResponse.success("订单已删除", null);
    }

    @Operation(summary = "确认收货")
    @PutMapping("/{orderId}/confirm-receipt")
    public ApiResponse<OrderDetailView> confirmReceipt(@PathVariable Long orderId) {
        return ApiResponse.success("确认收货成功", orderApplicationService.confirmReceipt(orderId));
    }

    @Operation(summary = "申请退款")
    @PostMapping("/{orderId}/refund")
    public ApiResponse<OrderRefundView> applyRefund(@PathVariable Long orderId, @Valid @RequestBody OrderRefundApplyRequest request) {
        return ApiResponse.success("退款申请已提交", orderApplicationService.applyRefund(orderId, request));
    }
}
