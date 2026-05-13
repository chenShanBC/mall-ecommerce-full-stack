package com.mallfei.stock.controller;

import com.mallfei.common.api.ApiResponse;
import com.mallfei.common.api.PageResult;
import com.mallfei.common.auth.RequireAdmin;
import com.mallfei.common.auth.RequireLogin;
import com.mallfei.stock.application.dto.StockAdjustRequest;
import com.mallfei.stock.application.dto.StockHealthView;
import com.mallfei.stock.application.dto.StockOperationRequest;
import com.mallfei.stock.application.dto.StockOperationResult;
import com.mallfei.stock.application.dto.StockQuery;
import com.mallfei.stock.application.dto.StockView;
import com.mallfei.stock.application.service.StockApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/stocks")
@Validated
@RequireLogin
@Tag(name = "库存内部接口")
public class StockController {

    private final StockApplicationService stockApplicationService;

    public StockController(StockApplicationService stockApplicationService) {
        this.stockApplicationService = stockApplicationService;
    }

    @Operation(summary = "库存模块健康检查")
    @GetMapping("/health")
    public ApiResponse<StockHealthView> health() {
        return ApiResponse.success(stockApplicationService.health());
    }

    @Operation(summary = "查询 SKU 库存")
    @GetMapping("/{skuId}")
    public ApiResponse<StockView> stock(@PathVariable Long skuId) { return ApiResponse.success(stockApplicationService.stockOf(skuId)); }

    @RequireAdmin
    @Operation(summary = "查询库存列表")
    @GetMapping
    public ApiResponse<PageResult<StockView>> stockList(@RequestParam(required = false) Long skuId, @RequestParam(required = false) String stockStatus, @RequestParam(required = false) String warningStatus, @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long size) { return ApiResponse.success(stockApplicationService.stockList(new StockQuery(skuId, stockStatus, warningStatus, "skuId", "asc", page, size))); }

    @RequireAdmin
    @Operation(summary = "初始化 SKU 库存")
    @PostMapping("/{skuId}/init")
    public ApiResponse<StockView> init(@PathVariable Long skuId, @RequestParam(defaultValue = "0") @Min(0) Integer initialStock) { return ApiResponse.success(stockApplicationService.initStock(skuId, initialStock)); }

    @RequireAdmin
    @Operation(summary = "更新库存策略")
    @PutMapping("/{skuId}/policy")
    public ApiResponse<StockView> updatePolicy(@PathVariable Long skuId, @RequestParam(required = false) String stockStatus, @RequestParam(required = false) Integer lowStockThreshold, @RequestParam(required = false) Integer highStockThreshold) { return ApiResponse.success(stockApplicationService.updateStockPolicy(skuId, stockStatus, lowStockThreshold, highStockThreshold)); }

    @RequireAdmin
    @Operation(summary = "手工调整库存数量")
    @PutMapping("/{skuId}/adjust")
    public ApiResponse<StockView> adjust(@PathVariable Long skuId, @Valid @RequestBody StockAdjustRequest request) { return ApiResponse.success(stockApplicationService.adjustStock(skuId, request)); }

    @Operation(summary = "同步 SKU 库存")
    @PostMapping("/{skuId}/sync")
    public ApiResponse<StockView> sync(@PathVariable Long skuId) { return ApiResponse.success(stockApplicationService.syncStock(skuId)); }

    @Operation(summary = "预占库存")
    @PostMapping("/reserve")
    public ApiResponse<StockOperationResult> reserve(@Valid @RequestBody StockOperationRequest request) { return ApiResponse.success(stockApplicationService.reserve(request)); }

    @Operation(summary = "取消预占库存")
    @DeleteMapping("/cancel")
    public ApiResponse<StockOperationResult> cancel(@Valid @RequestBody StockOperationRequest request) { return ApiResponse.success(stockApplicationService.cancel(request)); }

    @Operation(summary = "确认库存")
    @PutMapping("/confirm")
    public ApiResponse<StockOperationResult> confirm(@Valid @RequestBody StockOperationRequest request) { return ApiResponse.success(stockApplicationService.confirm(request)); }

    @Operation(summary = "直接回补库存")
    @PutMapping("/restore")
    public ApiResponse<StockOperationResult> restore(@Valid @RequestBody StockOperationRequest request) { return ApiResponse.success(stockApplicationService.restore(request)); }

    @Operation(summary = "锁定库存")
    @PostMapping("/lock")
    public ApiResponse<StockOperationResult> lock(@Valid @RequestBody StockOperationRequest request) { return ApiResponse.success(stockApplicationService.lock(request)); }

    @Operation(summary = "释放库存")
    @DeleteMapping("/release")
    public ApiResponse<StockOperationResult> release(@Valid @RequestBody StockOperationRequest request) { return ApiResponse.success(stockApplicationService.release(request)); }

    @Operation(summary = "扣减库存")
    @PatchMapping("/deduct")
    public ApiResponse<StockOperationResult> deduct(@Valid @RequestBody StockOperationRequest request) { return ApiResponse.success(stockApplicationService.deduct(request)); }
}
