package com.mallfei.admin.application.service;

import com.mallfei.admin.application.dto.AdminProductStatusOperateRequest;
import com.mallfei.admin.application.dto.AdminProductViolationHandleRequest;
import com.mallfei.product.application.vo.AdminProductSummaryView;
import com.mallfei.product.facade.ProductFacade;
import org.springframework.stereotype.Service;

@Service
public class AdminProductManagementApplicationService {

    private final ProductFacade productFacade;
    private final AdminAccountManagementApplicationService adminAccountManagementApplicationService;

    public AdminProductManagementApplicationService(ProductFacade productFacade,
                                                    AdminAccountManagementApplicationService adminAccountManagementApplicationService) {
        this.productFacade = productFacade;
        this.adminAccountManagementApplicationService = adminAccountManagementApplicationService;
    }

    public AdminProductSummaryView updateStatus(Long productId, AdminProductStatusOperateRequest request) {
        AdminProductSummaryView summary = productFacade.updateProductStatus(productId, request.status());
        adminAccountManagementApplicationService.recordOperation("PRODUCT", "PRODUCT_STATUS_UPDATE", "调整商品状态 productId=" + productId + " -> " + request.status() + "，原因=" + request.reason(), "SUCCESS");
        return summary;
    }

    public AdminProductSummaryView handleViolation(Long productId, AdminProductViolationHandleRequest request) {
        String action = request.action().toUpperCase();
        AdminProductSummaryView summary;
        if ("OFFLINE_VIOLATION".equals(action)) {
            summary = productFacade.updateProductStatus(productId, "OFFLINE");
        } else if ("RECOVER_ONLINE".equals(action)) {
            summary = productFacade.updateProductStatus(productId, "ONLINE");
        } else {
            throw com.mallfei.common.exception.BusinessException.badRequest("不支持的商品运营处理动作");
        }
        adminAccountManagementApplicationService.recordOperation("PRODUCT", "PRODUCT_VIOLATION_HANDLE", "处理商品运营事件 productId=" + productId + "，动作=" + action + "，原因=" + request.reason(), "SUCCESS");
        return summary;
    }
}
