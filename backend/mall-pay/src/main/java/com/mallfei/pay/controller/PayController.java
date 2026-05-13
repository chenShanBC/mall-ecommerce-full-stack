package com.mallfei.pay.controller;

import com.mallfei.common.api.ApiResponse;
import com.mallfei.common.auth.RequireLogin;
import com.mallfei.pay.application.service.PayApplicationService;
import com.mallfei.pay.application.vo.PayOrderView;
import com.mallfei.pay.application.vo.PayReconcileResultView;
import com.mallfei.pay.domain.service.PayChannelCallbackRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/pay")
@Tag(name = "支付")
public class PayController {

    private final PayApplicationService payApplicationService;

    public PayController(PayApplicationService payApplicationService) {
        this.payApplicationService = payApplicationService;
    }

    @RequireLogin
    @Operation(summary = "创建支付单")
    @PostMapping("/orders")
    public ApiResponse<PayOrderView> createPayOrder(@RequestParam String orderNo,
                                                    @RequestParam(required = false) String payChannel,
                                                    @RequestParam(required = false) String returnPath) {
        return ApiResponse.success(payApplicationService.createPayOrder(orderNo, payChannel, returnPath));
    }

    @Operation(summary = "模拟支付成功回调")
    @PostMapping("/callback/mock-success")
    public ApiResponse<PayOrderView> mockSuccess(@RequestParam String orderNo) {
        return ApiResponse.success(payApplicationService.markMockSuccess(orderNo));
    }

    @Operation(summary = "支付渠道异步回调")
    @PostMapping("/callback/channel")
    public ApiResponse<PayOrderView> handleChannelCallback(@RequestBody PayChannelCallbackRequest request) {
        return ApiResponse.success(payApplicationService.handleChannelCallback(request));
    }

    @Operation(summary = "支付宝异步回调")
    @PostMapping("/callback/alipay")
    public String handleAlipayCallback(@RequestParam Map<String, String> params) {
        return payApplicationService.handleAlipayCallback(params) ? "success" : "failure";
    }

    @RequireLogin
    @Operation(summary = "获取支付单详情")
    @GetMapping("/orders/{payOrderNo}")
    public ApiResponse<PayOrderView> detail(@PathVariable String payOrderNo) {
        return ApiResponse.success(payApplicationService.detail(payOrderNo));
    }

    @Operation(summary = "打开支付渠道提交页")
    @GetMapping(value = "/orders/{payOrderNo}/submit-page", produces = MediaType.TEXT_HTML_VALUE)
    public String submitPage(@PathVariable String payOrderNo,
                             @RequestParam(required = false) String returnPath) {
        return payApplicationService.renderPaySubmitPage(payOrderNo, returnPath);
    }

    @Operation(summary = "支付宝同步回跳桥接")
    @GetMapping(value = "/alipay/return-bridge", produces = MediaType.TEXT_HTML_VALUE)
    public String alipayReturnBridge(@RequestParam Map<String, String> params) {
        return payApplicationService.renderAlipayReturnBridge(params);
    }

    @GetMapping("/reconcile")
    public ApiResponse<PayReconcileResultView> reconcile(@RequestParam String orderNo) {
        return ApiResponse.success(payApplicationService.reconcile(orderNo));
    }

    @RequireLogin
    @Operation(summary = "同步订单支付状态")
    @PostMapping("/orders/{orderNo}/sync-status")
    public ApiResponse<PayOrderView> syncOrderStatus(@PathVariable String orderNo) {
        return ApiResponse.success(payApplicationService.syncOrderStatus(orderNo));
    }

    @RequireLogin
    @Operation(summary = "补偿已成功支付订单状态")
    @PostMapping("/orders/{orderNo}/repair-paid")
    public ApiResponse<PayOrderView> repairPaidOrder(@PathVariable String orderNo) {
        return ApiResponse.success(payApplicationService.repairPaidOrder(orderNo));
    }
}
