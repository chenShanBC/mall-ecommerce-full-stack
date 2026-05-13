package com.mallfei.admin.application.vo;

import java.util.List;

public record AdminLoginResult(
        String token,
        Long adminId,
        String username,
        String nickname,
        String roleCode,
        List<String> permissions
) {
}
