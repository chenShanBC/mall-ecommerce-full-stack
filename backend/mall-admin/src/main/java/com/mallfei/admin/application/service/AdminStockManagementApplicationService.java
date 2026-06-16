package com.mallfei.admin.application.service;

import com.mallfei.admin.application.dto.AdminStockAdjustRequest;
import com.mallfei.admin.application.dto.AdminStockPolicyUpdateRequest;
import com.mallfei.admin.application.dto.AdminStockWarningHandleRequest;
import com.mallfei.admin.application.vo.AdminStockOperationLogView;
import com.mallfei.stock.application.dto.StockAdjustRequest;
import com.mallfei.stock.application.vo.StockConsistencyCheckView;
import com.mallfei.stock.facade.StockFacade;
import com.mallfei.stock.facade.StockSnapshot;
import org.springframework.stereotype.Service;

@Service
public class AdminStockManagementApplicationService {

    private final StockFacade stockFacade;
    private final AdminAccountManagementApplicationService adminAccountManagementApplicationService;
    private final AdminQueryApplicationService adminQueryApplicationService;

    public AdminStockManagementApplicationService(StockFacade stockFacade,
                                                  AdminAccountManagementApplicationService adminAccountManagementApplicationService,
                                                  AdminQueryApplicationService adminQueryApplicationService) {
        this.stockFacade = stockFacade;
        this.adminAccountManagementApplicationService = adminAccountManagementApplicationService;
        this.adminQueryApplicationService = adminQueryApplicationService;
    }

    public StockSnapshot updatePolicy(Long skuId, AdminStockPolicyUpdateRequest request) {
        StockSnapshot before = stockFacade.stockOf(skuId);
        StockSnapshot snapshot = stockFacade.updateStockPolicy(skuId, request.stockStatus(), request.lowStockThreshold(), request.highStockThreshold(), request.reason());
        adminAccountManagementApplicationService.recordOperation("STOCK", "STOCK_POLICY_UPDATE", "调整库存策略 sku=" + skuId + "，" + stockPolicyChangeText(before, snapshot) + "，原因=" + request.reason(), "SUCCESS");
        return snapshot;
    }

    public StockSnapshot adjust(Long skuId, AdminStockAdjustRequest request) {
        StockSnapshot before = stockFacade.stockOf(skuId);
        StockSnapshot snapshot = stockFacade.adjustStock(skuId, new StockAdjustRequest(request.adjustmentType(), request.changeQuantity(), request.totalStock(), request.availableStock(), request.lockedStock(), request.reason(), request.remark()));
        adminAccountManagementApplicationService.recordOperation("STOCK", "STOCK_MANUAL_ADJUST", "手工调整库存 sku=" + skuId + "，类型=" + request.adjustmentType() + "，" + stockQuantityChangeText(before, snapshot) + "，原因=" + request.reason() + remarkText(request.remark()), "SUCCESS");
        return snapshot;
    }

    public StockSnapshot handleWarning(Long skuId, AdminStockWarningHandleRequest request) {
        String action = request.action().toUpperCase();
        StockSnapshot current = stockFacade.stockOf(skuId);
        StockSnapshot snapshot;
        if ("FREEZE".equals(action)) {
            snapshot = stockFacade.updateStockPolicy(skuId, "FROZEN", current.lowStockThreshold(), current.highStockThreshold(), request.note());
        } else if ("RECOVER_ACTIVE".equals(action)) {
            snapshot = stockFacade.updateStockPolicy(skuId, "ACTIVE", current.lowStockThreshold(), current.highStockThreshold(), request.note());
        } else if ("OFFLINE".equals(action)) {
            snapshot = stockFacade.updateStockPolicy(skuId, "OFFLINE", current.lowStockThreshold(), current.highStockThreshold(), request.note());
        } else {
            throw com.mallfei.common.exception.BusinessException.badRequest("不支持的预警处理动作");
        }
        adminAccountManagementApplicationService.recordOperation("STOCK", "STOCK_WARNING_HANDLE", "处理库存预警 sku=" + skuId + "，动作=" + action + "，" + stockPolicyChangeText(current, snapshot) + "，说明=" + request.note(), "SUCCESS");
        return snapshot;
    }

    public StockConsistencyCheckView checkConsistency(Long skuId) {
        StockConsistencyCheckView result = stockFacade.checkConsistency(skuId);
        adminAccountManagementApplicationService.recordOperation("STOCK", "STOCK_CONSISTENCY_CHECK", "发起库存一致性校验 sku=" + skuId + "，结果=" + result.status(), "SUCCESS");
        return result;
    }

    public com.mallfei.common.api.PageResult<com.mallfei.stock.application.vo.StockReconciliationRecordView> reconciliationRecords(Long skuId, String status, long page, long size, String sortBy, String sortOrder) {
        return stockFacade.pageReconciliationRecords(skuId, status, page, size, sortBy, sortOrder);
    }

    public com.mallfei.stock.application.vo.StockReconciliationRecordView reconciliationRecord(Long id) {
        return stockFacade.reconciliationRecord(id);
    }

    public com.mallfei.stock.application.vo.StockReconciliationRecordView repairReconciliationRecord(Long id, String remark) {
        var view = stockFacade.repairReconciliationRecord(id, remark);
        adminAccountManagementApplicationService.recordOperation("STOCK", "STOCK_RECONCILIATION_REPAIR", "修复库存对账记录 id=" + id + "，sku=" + view.skuId() + remarkText(remark), "SUCCESS");
        return view;
    }

    public com.mallfei.stock.application.vo.StockReconciliationRecordView ignoreReconciliationRecord(Long id, String remark) {
        var view = stockFacade.ignoreReconciliationRecord(id, remark);
        adminAccountManagementApplicationService.recordOperation("STOCK", "STOCK_RECONCILIATION_IGNORE", "忽略库存对账记录 id=" + id + "，sku=" + view.skuId() + remarkText(remark), "SUCCESS");
        return view;
    }

    public java.util.List<AdminStockOperationLogView> latestLogs(Long skuId) {
        return adminQueryApplicationService.stockLogs(skuId, null, null, null, 1, 20, null, null).records();
    }

    private String stockQuantityChangeText(StockSnapshot before, StockSnapshot after) {
        return "总库存 " + before.totalStock() + "->" + after.totalStock()
                + "，可用 " + before.availableStock() + "->" + after.availableStock()
                + "，锁定 " + before.lockedStock() + "->" + after.lockedStock();
    }

    private String stockPolicyChangeText(StockSnapshot before, StockSnapshot after) {
        return "状态 " + before.stockStatus() + "->" + after.stockStatus()
                + "，低库存阈值 " + before.lowStockThreshold() + "->" + after.lowStockThreshold()
                + "，高库存阈值 " + before.highStockThreshold() + "->" + after.highStockThreshold();
    }

    private String remarkText(String remark) {
        return remark == null || remark.isBlank() ? "" : "，备注=" + remark;
    }
}

