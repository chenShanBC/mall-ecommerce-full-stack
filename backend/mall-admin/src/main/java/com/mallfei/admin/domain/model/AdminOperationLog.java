package com.mallfei.admin.domain.model;

import java.time.LocalDateTime;

public record AdminOperationLog(
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
