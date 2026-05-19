package com.mallfei.user.domain.repository;

import com.mallfei.user.domain.model.UserThirdBind;

import java.util.Optional;

public interface UserThirdBindRepository {

    Optional<UserThirdBind> findByThirdTypeAndUid(String thirdType, String thirdUid);

    UserThirdBind save(UserThirdBind bind);
}
