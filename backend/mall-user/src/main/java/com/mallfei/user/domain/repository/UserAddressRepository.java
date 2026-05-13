package com.mallfei.user.domain.repository;

import com.mallfei.user.domain.model.UserAddress;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserAddressRepository {

    List<UserAddress> findByUserId(Long userId);

    Map<Long, List<UserAddress>> findByUserIds(List<Long> userIds);

    Optional<UserAddress> findById(Long id);

    UserAddress save(UserAddress userAddress);

    void update(UserAddress userAddress);

    void markDeleted(Long id);

    void clearDefaultByUserId(Long userId);

    long countActiveByUserId(Long userId);
}
