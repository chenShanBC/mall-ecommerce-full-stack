package com.mallfei.user.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mallfei.user.domain.model.UserAccount;
import com.mallfei.user.domain.repository.UserAccountRepository;
import com.mallfei.user.infrastructure.persistence.dataobject.UserAccountDO;
import com.mallfei.user.infrastructure.persistence.mapper.UserAccountMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisUserAccountRepository implements UserAccountRepository {

    private final UserAccountMapper userAccountMapper;

    public MybatisUserAccountRepository(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public Optional<UserAccount> findByMobile(String mobile) {
        UserAccountDO userAccountDO = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccountDO>()
                .eq(UserAccountDO::getMobile, mobile)
                .isNull(UserAccountDO::getDeletedAt)
                .last("limit 1"));
        return Optional.ofNullable(userAccountDO).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findById(Long id) {
        UserAccountDO userAccountDO = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccountDO>()
                .eq(UserAccountDO::getId, id)
                .isNull(UserAccountDO::getDeletedAt)
                .last("limit 1"));
        return Optional.ofNullable(userAccountDO).map(this::toDomain);
    }

    @Override
    public List<UserAccount> findAll() {
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccountDO>()
                        .isNull(UserAccountDO::getDeletedAt)
                        .orderByDesc(UserAccountDO::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByMobile(String mobile) {
        return findByMobile(mobile).isPresent();
    }

    @Override
    public boolean existsByNicknameAndIdNot(String nickname, Long userId) {
        Long count = userAccountMapper.selectCount(new LambdaQueryWrapper<UserAccountDO>()
                .eq(UserAccountDO::getNickname, nickname)
                .ne(UserAccountDO::getId, userId)
                .isNull(UserAccountDO::getDeletedAt));
        return count != null && count > 0;
    }

    @Override
    public boolean existsByMobileAndIdNot(String mobile, Long userId) {
        Long count = userAccountMapper.selectCount(new LambdaQueryWrapper<UserAccountDO>()
                .eq(UserAccountDO::getMobile, mobile)
                .ne(UserAccountDO::getId, userId)
                .isNull(UserAccountDO::getDeletedAt));
        return count != null && count > 0;
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
        UserAccountDO userAccountDO = new UserAccountDO();
        userAccountDO.setMobile(userAccount.mobile());
        userAccountDO.setPasswordHash(userAccount.passwordHash());
        userAccountDO.setNickname(userAccount.nickname());
        userAccountDO.setAvatarUrl(userAccount.avatarUrl());
        userAccountDO.setStatus(userAccount.status());
        userAccountMapper.insert(userAccountDO);
        return new UserAccount(
                userAccountDO.getId(),
                userAccountDO.getMobile(),
                userAccountDO.getPasswordHash(),
                userAccountDO.getNickname(),
                userAccountDO.getAvatarUrl(),
                userAccountDO.getStatus()
        );
    }

    @Override
    public UserAccount update(UserAccount userAccount) {
        UserAccountDO userAccountDO = new UserAccountDO();
        userAccountDO.setId(userAccount.id());
        userAccountDO.setMobile(userAccount.mobile());
        userAccountDO.setPasswordHash(userAccount.passwordHash());
        userAccountDO.setNickname(userAccount.nickname());
        userAccountDO.setAvatarUrl(userAccount.avatarUrl());
        userAccountDO.setStatus(userAccount.status());
        userAccountMapper.updateById(userAccountDO);
        return findById(userAccount.id()).orElseThrow();
    }

    @Override
    public void updatePasswordHash(Long userId, String passwordHash) {
        userAccountMapper.update(null, new LambdaUpdateWrapper<UserAccountDO>()
                .eq(UserAccountDO::getId, userId)
                .set(UserAccountDO::getPasswordHash, passwordHash));
    }

    private UserAccount toDomain(UserAccountDO userAccountDO) {
        return new UserAccount(
                userAccountDO.getId(),
                userAccountDO.getMobile(),
                userAccountDO.getPasswordHash(),
                userAccountDO.getNickname(),
                userAccountDO.getAvatarUrl(),
                userAccountDO.getStatus()
        );
    }
}
