package com.mallfei.admin.application.vo;

import java.util.List;

public record AdminUserDetailView(
        Long id,
        String mobile,
        String nickname,
        String avatarUrl,
        String status,
        List<AdminUserAddressView> addresses
) {
}
