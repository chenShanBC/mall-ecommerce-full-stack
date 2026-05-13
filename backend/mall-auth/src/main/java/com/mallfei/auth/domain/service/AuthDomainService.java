package com.mallfei.auth.domain.service;

import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.enums.IdentityType;
import com.mallfei.common.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class AuthDomainService {

    public void ensureLoggedIn(AuthenticatedPrincipal principal) {
        if (principal == null) {
            throw BusinessException.forbidden("当前未登录");
        }
    }

    public void ensureIdentity(AuthenticatedPrincipal principal, IdentityType identityType, String message) {
        ensureLoggedIn(principal);
        if (principal.identityType() != identityType) {
            throw BusinessException.forbidden(message);
        }
    }
}
