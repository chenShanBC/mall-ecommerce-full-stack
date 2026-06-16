package com.mallfei.auth.domain.model;

public record SessionConcurrencyPolicy(
        boolean enabled,
        boolean shareSameToken,
        boolean kickPreviousLogin
) {

    public static SessionConcurrencyPolicy disabled() {
        return new SessionConcurrencyPolicy(false, true, false);
    }

    public static SessionConcurrencyPolicy sameAccountKickPrevious(boolean shareSameToken) {
        return new SessionConcurrencyPolicy(true, shareSameToken, true);
    }
}
