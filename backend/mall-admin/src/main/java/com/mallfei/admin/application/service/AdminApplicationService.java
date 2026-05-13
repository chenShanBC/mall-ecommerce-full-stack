package com.mallfei.admin.application.service;

import com.mallfei.admin.application.dto.AdminChangePasswordRequest;
import com.mallfei.admin.application.dto.AdminPasswordLoginRequest;
import com.mallfei.admin.application.dto.AdminUpdateProfileRequest;
import com.mallfei.admin.application.vo.AdminLoginResult;
import com.mallfei.admin.domain.model.AdminAccount;
import com.mallfei.admin.domain.repository.AdminAccountRepository;
import com.mallfei.admin.domain.service.AdminDomainService;
import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.enums.IdentityType;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AdminApplicationService {

    private final AdminDomainService adminDomainService;
    private final AdminAccountRepository adminAccountRepository;
    private final AuthFacade authFacade;

    public AdminApplicationService(AdminDomainService adminDomainService,
                                   AdminAccountRepository adminAccountRepository,
                                   AuthFacade authFacade) {
        this.adminDomainService = adminDomainService;
        this.adminAccountRepository = adminAccountRepository;
        this.authFacade = authFacade;
    }

    public AdminLoginResult login(AdminPasswordLoginRequest request) {
        AdminAccount adminAccount = adminDomainService.loadByUsername(request.username());
        adminDomainService.validateLogin(adminAccount, request.password());
        String token = authFacade.createLoginSession(
                adminAccount.id(),
                adminAccount.username(),
                IdentityType.ADMIN,
                adminAccount.nickname(),
                "https://via.placeholder.com/120x120.png?text=admin",
                adminAccount.roleCode(),
                adminAccount.permissions()
        );
        return new AdminLoginResult(token, adminAccount.id(), adminAccount.username(), adminAccount.nickname(), adminAccount.roleCode(), adminAccount.permissions());
    }

    public AuthenticatedPrincipal currentAdmin() {
        AuthenticatedPrincipal principal = authFacade.currentPrincipal();
        AdminAccount adminAccount = adminDomainService.loadById(principal.principalId());
        return new AuthenticatedPrincipal(
                principal.loginId(),
                adminAccount.id(),
                adminAccount.username(),
                principal.identityType(),
                adminAccount.nickname(),
                "https://via.placeholder.com/120x120.png?text=admin",
                principal.token(),
                adminAccount.roleCode(),
                adminAccount.permissions()
        );
    }

    public AuthenticatedPrincipal updateProfile(AdminUpdateProfileRequest request) {
        AuthenticatedPrincipal principal = authFacade.currentPrincipal();
        AdminAccount adminAccount = adminDomainService.loadById(principal.principalId());
        AdminAccount updated = adminAccountRepository.update(adminAccount.rename(request.nickname()));
        authFacade.refreshAdminSession(updated.nickname(), updated.roleCode(), updated.permissions());
        return currentAdmin();
    }

    public Boolean changePassword(AdminChangePasswordRequest request) {
        AuthenticatedPrincipal principal = authFacade.currentPrincipal();
        AdminAccount adminAccount = adminDomainService.loadById(principal.principalId());
        adminDomainService.validateOldPassword(adminAccount, request.oldPassword());
        adminAccountRepository.update(adminAccount.changePassword(adminDomainService.encodePassword(request.newPassword())));
        return Boolean.TRUE;
    }

    public void logout() {
        authFacade.logout();
    }

    public Map<String, Object> dashboard() {
        AuthenticatedPrincipal principal = currentAdmin();
        return Map.of(
                "nickname", principal.nickname(),
                "productCount", 1,
                "userCount", 1,
                "orderCount", 1,
                "pendingOrderCount", 1
        );
    }
}
