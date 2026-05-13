package com.mallfei.user.domain.repository;

import com.mallfei.user.domain.model.UserAccount;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository {

    Optional<UserAccount> findByMobile(String mobile);

    Optional<UserAccount> findById(Long id);

    List<UserAccount> findAll();

    boolean existsByMobile(String mobile);

    boolean existsByNicknameAndIdNot(String nickname, Long userId);

    UserAccount save(UserAccount userAccount);

    UserAccount update(UserAccount userAccount);

    void updatePasswordHash(Long userId, String passwordHash);

    boolean existsByMobileAndIdNot(String mobile, Long userId);
}
