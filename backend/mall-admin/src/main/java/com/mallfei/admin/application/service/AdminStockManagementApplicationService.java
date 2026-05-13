package com.mallfei.admin.application.service;

import com.mallfei.admin.application.dto.AdminStockAdjustRequest;
import com.mallfei.admin.application.dto.AdminStockPolicyUpdateRequest;
import com.mallfei.admin.application.dto.AdminStockWarningHandleRequest;
import com.mallfei.admin.application.vo.AdminStockOperationLogView;
import com.mallfei.stock.application.dto.StockAdjustRequest;
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
        StockSnapshot snapshot = stockFacade.updateStockPolicy(skuId, request.stockStatus(), request.lowStockThreshold(), request.highStockThreshold(), request.reason());
        adminAccountManagementApplicationService.recordOperation("STOCK", "STOCK_POLICY_UPDATE", "调整库存策略 sku=" + skuId + "，原因=" + request.reason(), "SUCCESS");
        return snapshot;
    }

    public StockSnapshot adjust(Long skuId, AdminStockAdjustRequest request) {
        StockSnapshot snapshot = stockFacade.adjustStock(skuId, new StockAdjustRequest(request.totalStock(), request.availableStock(), request.lockedStock(), request.reason()));
        adminAccountManagementApplicationService.recordOperation("STOCK", "STOCK_MANUAL_ADJUST", "手工调整库存 sku=" + skuId + "，原因=" + request.reason(), "SUCCESS");
        return snapshot;
    }

    public StockSnapshot handleWarning(Long skuId, AdminStockWarningHandleRequest request) {
        String action = request.action().toUpperCase();
        StockSnapshot current = stockFacade.stockOf(skuId);
        StockSnapshot snapshot;
        if ("FREEZE".equals(action)) {
            snapshot = stockFacade.updateStockPolicy(skuId, "FROZEN", current.lowStockThreshold(), current.highStockThreshold());
        } else if ("RECOVER_ACTIVE".equals(action)) {
            snapshot = stockFacade.updateStockPolicy(skuId, "ACTIVE", current.lowStockThreshold(), current.highStockThreshold());
        } else if ("OFFLINE".equals(action)) {
            snapshot = stockFacade.updateStockPolicy(skuId, "OFFLINE", current.lowStockThreshold(), current.highStockThreshold());
        } else {
            throw com.mallfei.common.exception.BusinessException.badRequest("不支持的预警处理动作");
        }
        adminAccountManagementApplicationService.recordOperation("STOCK", "STOCK_WARNING_HANDLE", "处理库存预警 sku=" + skuId + "，动作=" + action + "，说明=" + request.note(), "SUCCESS");
        return snapshot;
    }

    public java.util.List<AdminStockOperationLogView> latestLogs(Long skuId) {
        return adminQueryApplicationService.stockLogs(skuId, null, null, null, 1, 20, null, null).records();
    }
}
