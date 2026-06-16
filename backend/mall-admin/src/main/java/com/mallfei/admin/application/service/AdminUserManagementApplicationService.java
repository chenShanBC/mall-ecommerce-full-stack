package com.mallfei.admin.application.service;

import com.mallfei.admin.application.vo.AdminUserAddressView;
import com.mallfei.admin.application.vo.AdminUserDetailView;
import com.mallfei.admin.application.vo.AdminUserListItemView;
import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.common.api.PageResult;
import com.mallfei.user.domain.model.UserAccount;
import com.mallfei.user.domain.model.UserAddress;
import com.mallfei.user.domain.repository.UserAccountRepository;
import com.mallfei.user.domain.repository.UserAddressRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class AdminUserManagementApplicationService {

    private final UserAccountRepository userAccountRepository;
    private final UserAddressRepository userAddressRepository;
    private final AdminAccountManagementApplicationService adminAccountManagementApplicationService;
    private final AuthFacade authFacade;

    public AdminUserManagementApplicationService(UserAccountRepository userAccountRepository,
                                                 UserAddressRepository userAddressRepository,
                                                 AdminAccountManagementApplicationService adminAccountManagementApplicationService,
                                                 AuthFacade authFacade) {
        this.userAccountRepository = userAccountRepository;
        this.userAddressRepository = userAddressRepository;
        this.adminAccountManagementApplicationService = adminAccountManagementApplicationService;
        this.authFacade = authFacade;
    }

    public PageResult<AdminUserListItemView> users(String keyword, String status, String sortBy, String sortOrder, long page, long size) {
        List<UserAccount> accounts = userAccountRepository.findAll().stream()
                .filter(item -> blank(status) || status.equalsIgnoreCase(item.status()))
                .filter(item -> blank(keyword) || contains(item.mobile(), keyword) || contains(item.nickname(), keyword))
                .sorted(userComparator(sortBy, sortOrder))
                .toList();
        Map<Long, List<UserAddress>> addressesByUserId = userAddressRepository.findByUserIds(accounts.stream().map(UserAccount::id).toList());
        List<AdminUserListItemView> rows = accounts.stream()
                .map(item -> toListView(item, addressesByUserId.getOrDefault(item.id(), List.of())))
                .toList();
        return PageResult.of(rows, page, size);
    }

    public AdminUserDetailView userDetail(Long userId) {
        UserAccount user = userAccountRepository.findById(userId).orElseThrow();
        List<UserAddress> addresses = userAddressRepository.findByUserId(userId);
        return new AdminUserDetailView(
                user.id(),
                user.mobile(),
                user.nickname(),
                user.avatarUrl(),
                user.status(),
                addresses.stream().map(this::toAddressView).toList()
        );
    }

    public AdminUserDetailView disableUser(Long userId) {
        UserAccount updated = userAccountRepository.update(userAccountRepository.findById(userId).orElseThrow().disable());
        authFacade.disableUserSession(userId);
        adminAccountManagementApplicationService.recordOperation("USER", "USER_DISABLE", "禁用C端用户：userId=" + userId, "SUCCESS");
        return userDetail(updated.id());
    }

    public AdminUserDetailView enableUser(Long userId) {
        UserAccount updated = userAccountRepository.update(userAccountRepository.findById(userId).orElseThrow().enable());
        authFacade.enableUserSession(userId);
        adminAccountManagementApplicationService.recordOperation("USER", "USER_ENABLE", "启用C端用户：userId=" + userId, "SUCCESS");
        return userDetail(updated.id());
    }

    private AdminUserListItemView toListView(UserAccount user, List<UserAddress> addresses) {
        UserAddress defaultAddress = addresses.stream().filter(UserAddress::isDefault).findFirst().orElse(addresses.stream().findFirst().orElse(null));
        return new AdminUserListItemView(
                user.id(),
                user.mobile(),
                user.nickname(),
                user.status(),
                addresses.size(),
                defaultAddress == null ? "-" : defaultAddress.receiverName(),
                defaultAddress == null ? "-" : defaultAddress.receiverPhone(),
                defaultAddress == null ? "-" : formatAddress(defaultAddress),
                List.of(addresses.isEmpty() ? "无地址" : "有地址", user.enabled() ? "正常用户" : "已禁用")
        );
    }

    private Comparator<UserAccount> userComparator(String sortBy, String sortOrder) {
        Comparator<UserAccount> comparator = switch (blank(sortBy) ? "id" : sortBy) {
            case "id" -> Comparator.comparing(UserAccount::id, Comparator.nullsLast(Long::compareTo));
            case "mobile" -> Comparator.comparing(UserAccount::mobile, Comparator.nullsLast(String::compareTo));
            case "nickname" -> Comparator.comparing(UserAccount::nickname, Comparator.nullsLast(String::compareTo));
            case "status" -> Comparator.comparing(UserAccount::status, Comparator.nullsLast(String::compareTo));
            default -> Comparator.comparing(UserAccount::id, Comparator.nullsLast(Long::compareTo));
        };
        return "desc".equalsIgnoreCase(sortOrder) ? comparator.reversed() : comparator;
    }

    private AdminUserAddressView toAddressView(UserAddress address) {
        return new AdminUserAddressView(address.id(), address.receiverName(), address.receiverPhone(), formatAddress(address), address.isDefault());
    }

    private String formatAddress(UserAddress address) {
        return String.join(" ", List.of(
                safe(address.provinceName()),
                safe(address.cityName()),
                safe(address.districtName()),
                safe(address.detailAddress())
        )).trim().replaceAll("\\s+", " ");
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private boolean contains(String source, String keyword) { return source != null && source.toLowerCase().contains(keyword.toLowerCase().trim()); }
    private String safe(String value) { return value == null ? "" : value; }
}
