package com.mallfei.user.facade;

import com.mallfei.user.application.dto.UserAddressCreateRequest;
import com.mallfei.user.application.dto.UserAddressUpdateRequest;
import com.mallfei.user.application.dto.UserPasswordLoginRequest;
import com.mallfei.user.application.dto.UserRegisterRequest;
import com.mallfei.user.application.service.UserApplicationService;
import com.mallfei.user.application.vo.UserLoginResult;
import com.mallfei.user.application.vo.UserProfileVO;
import com.mallfei.user.domain.model.UserAddress;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserFacade {

    private final UserApplicationService userApplicationService;

    public UserFacade(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    public UserLoginResult login(UserPasswordLoginRequest request) {
        return userApplicationService.login(request);
    }

    public UserLoginResult register(UserRegisterRequest request) {
        return userApplicationService.register(request);
    }

    public UserProfileVO currentUser() {
        return userApplicationService.currentUser();
    }

    public void logout() {
        userApplicationService.logout();
    }

    public List<UserAddress> currentUserAddresses() {
        return userApplicationService.currentUserAddresses();
    }

    public UserAddress createAddress(UserAddressCreateRequest request) {
        return userApplicationService.createAddress(request);
    }

    public UserAddress updateAddress(Long addressId, UserAddressUpdateRequest request) {
        return userApplicationService.updateAddress(addressId, request);
    }

    public void deleteAddress(Long addressId) {
        userApplicationService.deleteAddress(addressId);
    }

    public UserAddress setDefaultAddress(Long addressId) {
        return userApplicationService.setDefaultAddress(addressId);
    }
}
