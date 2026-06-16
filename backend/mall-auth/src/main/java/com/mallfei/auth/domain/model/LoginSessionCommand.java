package com.mallfei.auth.domain.model;

import com.mallfei.common.enums.IdentityType;

import java.util.List;

public record LoginSessionCommand(
        Long principalId,
        String account,
        IdentityType identityType,
        String nickname,
        String avatar,
        String roleCode,
        List<String> permissions,
        AuthDeviceType deviceType
) {

    public LoginSessionCommand {
        if (principalId == null) {
            throw new IllegalArgumentException("principalId must not be null");
        }
        if (identityType == null) {
            throw new IllegalArgumentException("identityType must not be null");
        }
        if (deviceType == null) {
            deviceType = AuthDeviceType.defaultOf(identityType);
        }
        if (permissions == null) {
            permissions = List.of();
        }
    }

    public String loginId() {
        return identityType.code() + ":" + principalId;
    }
}
