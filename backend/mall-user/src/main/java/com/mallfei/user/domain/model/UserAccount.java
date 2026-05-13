package com.mallfei.user.domain.model;

import com.mallfei.common.exception.BusinessException;

public record UserAccount(
        Long id,
        String mobile,
        String passwordHash,
        String nickname,
        String avatarUrl,
        String status
) {

    public boolean passwordMatches(String rawPassword) {
        return passwordHash.equals(rawPassword);
    }

    public boolean enabled() {
        return "ENABLED".equalsIgnoreCase(status);
    }

    public UserAccount disable() {
        if (!enabled()) {
            return this;
        }
        return new UserAccount(id, mobile, passwordHash, nickname, avatarUrl, "DISABLED");
    }

    public UserAccount enable() {
        if (enabled()) {
            return this;
        }
        return new UserAccount(id, mobile, passwordHash, nickname, avatarUrl, "ENABLED");
    }

    public UserAccount withNickname(String newNickname) {
        if (newNickname == null || newNickname.isBlank()) {
            throw BusinessException.badRequest("昵称不能为空");
        }
        return new UserAccount(
                id,
                mobile,
                passwordHash,
                newNickname,
                avatarUrl,
                status
        );
    }

    public UserAccount withProfile(String newNickname, String newAvatarUrl) {
        return new UserAccount(
                id,
                mobile,
                passwordHash,
                newNickname,
                newAvatarUrl,
                status
        );
    }

    public UserAccount withPasswordHash(String newPasswordHash) {
        return new UserAccount(
                id,
                mobile,
                newPasswordHash,
                nickname,
                avatarUrl,
                status
        );
    }
}
