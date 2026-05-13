package com.mallfei.user.controller;

import com.mallfei.common.api.ApiResponse;
import com.mallfei.common.auth.RequireUser;
import com.mallfei.user.application.dto.UserAddressCreateRequest;
import com.mallfei.user.application.dto.UserAddressUpdateRequest;
import com.mallfei.user.application.service.UserApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/addresses")
@RequireUser
@Tag(name = "用户地址")
public class UserAddressController {

    private final UserApplicationService userApplicationService;

    public UserAddressController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    @Operation(summary = "获取当前用户地址列表")
    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.success(userApplicationService.currentUserAddresses());
    }

    @Operation(summary = "新增用户地址")
    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody UserAddressCreateRequest request) {
        return ApiResponse.success(userApplicationService.createAddress(request));
    }

    @Operation(summary = "修改用户地址")
    @PutMapping("/{addressId}")
    public ApiResponse<?> update(@PathVariable Long addressId, @Valid @RequestBody UserAddressUpdateRequest request) {
        return ApiResponse.success(userApplicationService.updateAddress(addressId, request));
    }

    @Operation(summary = "删除用户地址")
    @DeleteMapping("/{addressId}")
    public ApiResponse<?> delete(@PathVariable Long addressId) {
        userApplicationService.deleteAddress(addressId);
        return ApiResponse.success("删除成功", Boolean.TRUE);
    }

    @Operation(summary = "设置默认地址")
    @PutMapping("/{addressId}/default")
    public ApiResponse<?> setDefault(@PathVariable Long addressId) {
        return ApiResponse.success(userApplicationService.setDefaultAddress(addressId));
    }
}
