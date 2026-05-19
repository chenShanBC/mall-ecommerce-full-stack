package com.mallfei.user.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallfei.user.domain.model.UserThirdBind;
import com.mallfei.user.domain.repository.UserThirdBindRepository;
import com.mallfei.user.infrastructure.persistence.dataobject.UserThirdBindDO;
import com.mallfei.user.infrastructure.persistence.mapper.UserThirdBindMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MybatisUserThirdBindRepository implements UserThirdBindRepository {

    private final UserThirdBindMapper mapper;

    public MybatisUserThirdBindRepository(UserThirdBindMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<UserThirdBind> findByThirdTypeAndUid(String thirdType, String thirdUid) {
        UserThirdBindDO bindDO = mapper.selectOne(new LambdaQueryWrapper<UserThirdBindDO>()
                .eq(UserThirdBindDO::getThirdType, thirdType)
                .eq(UserThirdBindDO::getThirdUid, thirdUid)
                .last("limit 1"));
        return Optional.ofNullable(bindDO).map(this::toDomain);
    }

    @Override
    public UserThirdBind save(UserThirdBind bind) {
        UserThirdBindDO bindDO = new UserThirdBindDO();
        bindDO.setUserId(bind.userId());
        bindDO.setThirdType(bind.thirdType());
        bindDO.setThirdUid(bind.thirdUid());
        bindDO.setThirdNickname(bind.thirdNickname());
        bindDO.setThirdAvatar(bind.thirdAvatar());
        mapper.insert(bindDO);
        return toDomain(bindDO);
    }

    private UserThirdBind toDomain(UserThirdBindDO bindDO) {
        return new UserThirdBind(
                bindDO.getId(),
                bindDO.getUserId(),
                bindDO.getThirdType(),
                bindDO.getThirdUid(),
                bindDO.getThirdNickname(),
                bindDO.getThirdAvatar()
        );
    }
}
