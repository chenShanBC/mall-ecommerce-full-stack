package com.mallfei.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminReconciliationHandleResultView", description = "后台对账差异处置结果")
public record AdminReconciliationHandleResultView(
        @Schema(description = "处置动作", example = "SYNC_PAY_STATUS")
        String action,
        @Schema(description = "处理结果", example = "SUCCESS")
        String status,
        @Schema(description = "结果说明")
        String message,
        @Schema(description = "最新对账记录")
        AdminPayReconciliationRecordView record
) {
}
