package com.mallfei.auth.domain.service;

import com.mallfei.auth.domain.model.AuthDeviceType;
import com.mallfei.auth.domain.model.SessionConcurrencyPolicy;
import com.mallfei.auth.infrastructure.config.AuthSessionProperties;
import com.mallfei.common.enums.IdentityType;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionPolicyService {

    private final AuthSessionProperties properties;

    public AuthSessionPolicyService(AuthSessionProperties properties) {
        this.properties = properties;
    }

    public SessionConcurrencyPolicy concurrencyPolicy(IdentityType identityType, AuthDeviceType deviceType) {
        if (identityType != null && identityType.isAdmin()) {
            return properties.admin().singleLogin()
                    ? SessionConcurrencyPolicy.sameAccountKickPrevious(properties.shareSameToken())
                    : SessionConcurrencyPolicy.disabled();
        }
        return properties.user().singleLogin()
                ? SessionConcurrencyPolicy.sameAccountKickPrevious(properties.shareSameToken())
                : SessionConcurrencyPolicy.disabled();
    }
}
