package com.mallfei.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mall.auth.session")
public class AuthSessionProperties {

    /**
     * Same browser tabs reuse one token when they login with the same account and device type.
     */
    private boolean shareSameToken = true;

    private PrincipalSessionPolicy admin = new PrincipalSessionPolicy(true);

    /**
     * User side remains configurable to avoid changing consumer multi-device behavior accidentally.
     */
    private PrincipalSessionPolicy user = new PrincipalSessionPolicy(false);

    public boolean shareSameToken() {
        return shareSameToken;
    }

    public void setShareSameToken(boolean shareSameToken) {
        this.shareSameToken = shareSameToken;
    }

    public PrincipalSessionPolicy admin() {
        return admin;
    }

    public void setAdmin(PrincipalSessionPolicy admin) {
        this.admin = admin == null ? new PrincipalSessionPolicy(true) : admin;
    }

    public PrincipalSessionPolicy user() {
        return user;
    }

    public void setUser(PrincipalSessionPolicy user) {
        this.user = user == null ? new PrincipalSessionPolicy(false) : user;
    }

    public static class PrincipalSessionPolicy {

        private boolean singleLogin;

        public PrincipalSessionPolicy() {
        }

        public PrincipalSessionPolicy(boolean singleLogin) {
            this.singleLogin = singleLogin;
        }

        public boolean singleLogin() {
            return singleLogin;
        }

        public void setSingleLogin(boolean singleLogin) {
            this.singleLogin = singleLogin;
        }
    }
}
