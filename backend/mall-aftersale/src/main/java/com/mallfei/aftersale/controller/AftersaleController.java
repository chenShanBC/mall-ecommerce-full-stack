package com.mallfei.aftersale.controller;

import com.mallfei.aftersale.application.dto.AftersaleRefundApplyRequest;
import com.mallfei.aftersale.application.service.AftersaleApplicationService;
import com.mallfei.aftersale.application.vo.AftersaleRefundApplyView;
import com.mallfei.common.api.ApiResponse;
import com.mallfei.common.auth.RequireUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aftersales")
@RequireUser
@Tag(name = "售后")
public class AftersaleController {

    private final AftersaleApplicationService aftersaleApplicationService;

    public AftersaleController(AftersaleApplicationService aftersaleApplicationService) {
        this.aftersaleApplicationService = aftersaleApplicationService;
    }

    @Operation(summary = "发起仅退款售后申请")
    @PostMapping("/refund")
    public ApiResponse<AftersaleRefundApplyView> applyRefund(@Valid @RequestBody AftersaleRefundApplyRequest request) {
        return ApiResponse.success("售后申请已提交", aftersaleApplicationService.applyOnlyRefund(request));
    }
}
