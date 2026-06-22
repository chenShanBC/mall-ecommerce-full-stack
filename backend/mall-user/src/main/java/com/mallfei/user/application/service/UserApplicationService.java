package com.mallfei.user.application.service;

import com.mallfei.auth.domain.model.AuthDeviceType;
import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.enums.IdentityType;
import com.mallfei.user.application.dto.LoginCaptchaVerifyRequest;
import com.mallfei.user.application.dto.UserAddressCreateRequest;
import com.mallfei.user.application.dto.UserAddressUpdateRequest;
import com.mallfei.user.application.dto.UserMobileBindCodeSendRequest;
import com.mallfei.user.application.dto.UserMobileBindRequest;
import com.mallfei.user.application.dto.UserPasswordChangeRequest;
import com.mallfei.user.application.dto.UserPasswordLoginRequest;
import com.mallfei.user.application.dto.UserProfileUpdateRequest;
import com.mallfei.user.application.dto.UserRegisterRequest;
import com.mallfei.user.application.dto.UserSmsCodeLoginRequest;
import com.mallfei.user.application.dto.UserSmsCodeSendRequest;
import com.mallfei.user.application.vo.LoginCaptchaChallengeResult;
import com.mallfei.user.application.vo.LoginCaptchaVerifyResult;
import com.mallfei.user.application.vo.SmsCodeSendResult;
import com.mallfei.user.application.vo.UserLoginResult;
import com.mallfei.user.application.vo.UserProfileVO;
import com.mallfei.user.domain.model.UserAccount;
import com.mallfei.user.domain.model.UserAddress;
import com.mallfei.user.domain.service.UserDomainService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserApplicationService {

    private final UserDomainService userDomainService;
    private final AuthFacade authFacade;

    public UserApplicationService(UserDomainService userDomainService,
                                  AuthFacade authFacade) {
        this.userDomainService = userDomainService;
        this.authFacade = authFacade;
    }

    public boolean blacklistStatusByMobile(String mobile) {
        UserAccount userAccount = userDomainService.loadByMobile(mobile);
        return authFacade.isUserDisabled(userAccount.id()) || !userAccount.enabled();
    }

    public LoginCaptchaChallengeResult createLoginCaptchaChallenge() {
        return userDomainService.createLoginCaptchaChallenge();
    }

    public LoginCaptchaVerifyResult verifyLoginCaptcha(LoginCaptchaVerifyRequest request) {
        return userDomainService.verifyLoginCaptcha(request.captchaToken(), request.offset());
    }

    public UserLoginResult login(UserPasswordLoginRequest request) {
        UserAccount userAccount = userDomainService.loadByMobile(request.mobile());
        ensureEnabled(userAccount);
        userDomainService.consumeVerifiedLoginCaptcha(request.captchaToken(), request.captchaVerifyToken());
        userDomainService.validateLogin(userAccount, request.password());
        return buildLoginResult(userAccount);
    }

    public SmsCodeSendResult sendLoginSmsCode(UserSmsCodeSendRequest request) {
        return userDomainService.sendLoginSmsCode(request.mobile());
    }

    public UserLoginResult loginBySmsCode(UserSmsCodeLoginRequest request) {
        UserAccount userAccount = userDomainService.loadByMobile(request.mobile());
        ensureEnabled(userAccount);
        userAccount = userDomainService.loginBySmsCode(request.mobile(), request.code());
        return buildLoginResult(userAccount);
    }

    public UserLoginResult register(UserRegisterRequest request) {
        UserAccount userAccount = userDomainService.register(request.mobile(), request.password(), request.nickname());
        return buildLoginResult(userAccount);
    }

    public UserProfileVO currentUser() {
        AuthenticatedPrincipal principal = authFacade.currentPrincipal();
        UserAccount userAccount = userDomainService.loadById(principal.principalId());
        ensureEnabled(userAccount);
        return toProfileVO(userAccount);
    }

    public UserProfileVO updateCurrentUserProfile(UserProfileUpdateRequest request) {
        UserAccount updated = userDomainService.updateProfile(currentUserId(), request.nickname(), request.avatarUrl());
        return toProfileVO(updated);
    }

    public UserProfileVO updateCurrentUserAvatar(String avatarUrl) {
        UserAccount existing = userDomainService.loadById(currentUserId());
        UserAccount updated = userDomainService.updateProfile(currentUserId(), existing.nickname(), avatarUrl);
        return toProfileVO(updated);
    }

    public void changeCurrentUserPassword(UserPasswordChangeRequest request) {
        userDomainService.changePassword(currentUserId(), request.oldPassword(), request.newPassword(), request.confirmPassword());
        authFacade.logout();
    }

    public SmsCodeSendResult sendMobileBindSmsCode(UserMobileBindCodeSendRequest request) {
        return userDomainService.sendMobileBindSmsCode(currentUserId(), request.mobile());
    }

    public UserProfileVO bindCurrentUserMobile(UserMobileBindRequest request) {
        UserAccount updated = userDomainService.bindMobile(currentUserId(), request.mobile(), request.code());
        return toProfileVO(updated);
    }

    public void logout() {
        authFacade.logout();
    }

    public List<UserAddress> currentUserAddresses() {
        return userDomainService.loadAddresses(currentUserId());
    }

    public UserAddress createAddress(UserAddressCreateRequest request) {
        Long userId = currentUserId();
        return userDomainService.createAddress(
                userId,
                request.receiverName(),
                request.receiverPhone(),
                request.provinceCode(),
                request.provinceName(),
                request.cityCode(),
                request.cityName(),
                request.districtCode(),
                request.districtName(),
                request.detailAddress(),
                request.postalCode(),
                request.isDefault()
        );
    }

    public UserAddress updateAddress(Long addressId, UserAddressUpdateRequest request) {
        UserAddress existing = userDomainService.loadOwnedAddress(addressId, currentUserId());
        return userDomainService.updateAddress(
                existing,
                request.receiverName(),
                request.receiverPhone(),
                request.provinceCode(),
                request.provinceName(),
                request.cityCode(),
                request.cityName(),
                request.districtCode(),
                request.districtName(),
                request.detailAddress(),
                request.postalCode(),
                request.isDefault()
        );
    }

    public void deleteAddress(Long addressId) {
        UserAddress existing = userDomainService.loadOwnedAddress(addressId, currentUserId());
        userDomainService.deleteAddress(existing);
    }

    public UserAddress setDefaultAddress(Long addressId) {
        UserAddress existing = userDomainService.loadOwnedAddress(addressId, currentUserId());
        return userDomainService.setDefaultAddress(existing);
    }

    private void ensureEnabled(UserAccount userAccount) {
        if (authFacade.isUserDisabled(userAccount.id()) || !userAccount.enabled()) {
            authFacade.disableUserSession(userAccount.id());
            throw new com.mallfei.common.exception.BusinessException(UserDomainService.USER_DISABLED_CODE, UserDomainService.USER_DISABLED_MESSAGE);
        }
    }

    private Long currentUserId() {
        return authFacade.currentPrincipal().principalId();
    }

    private UserProfileVO toProfileVO(UserAccount userAccount) {
        return new UserProfileVO(
                userAccount.id(),
                userAccount.mobile(),
                userAccount.nickname(),
                userAccount.avatarUrl(),
                "普通用户"
        );
    }

    private UserLoginResult buildLoginResult(UserAccount userAccount) {
        String token = authFacade.createLoginSession(
                userAccount.id(),
                userAccount.mobile(),
                IdentityType.USER,
                userAccount.nickname(),
                userAccount.avatarUrl(),
                "USER",
                List.of(),
                AuthDeviceType.USER_H5
        );
        return new UserLoginResult(token, userAccount.id(), userAccount.mobile(), userAccount.nickname(), userAccount.avatarUrl(), userAccount.mobile() != null && !userAccount.mobile().isBlank());
    }
}
