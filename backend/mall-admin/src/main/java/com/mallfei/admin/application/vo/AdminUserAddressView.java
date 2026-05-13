package com.mallfei.admin.application.vo;

public record AdminUserAddressView(
        Long id,
        String receiverName,
        String receiverPhone,
        String fullAddress,
        boolean isDefault
) {
}
