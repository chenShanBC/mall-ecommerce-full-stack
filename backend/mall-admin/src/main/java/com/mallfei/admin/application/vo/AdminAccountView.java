package com.mallfei.admin.application.vo;

import java.time.LocalDateTime;
import java.util.List;

public record AdminAccountView(
        Long id,
        Long userId,
        String username,
        String nickname,
        String roleCode,
        String status,
        List<String> permissions,
        LocalDateTime createdAt
) {
}
