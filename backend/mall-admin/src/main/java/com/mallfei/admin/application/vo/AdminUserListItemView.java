package com.mallfei.admin.application.vo;

import java.util.List;

public record AdminUserListItemView(
        Long id,
        String mobile,
        String nickname,
        String status,
        int addressCount,
        String defaultReceiverName,
        String defaultReceiverPhone,
        String defaultAddress,
        List<String> tags
) {
}
