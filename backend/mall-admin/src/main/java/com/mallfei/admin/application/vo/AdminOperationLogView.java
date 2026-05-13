package com.mallfei.admin.application.vo;

import java.time.LocalDateTime;

public record AdminOperationLogView(
        Long id,
        Long operatorAdminId,
        String operatorUsername,
        String operationModule,
        String operationType,
        String operationContent,
        String operationResult,
        LocalDateTime createdAt
) {
}
