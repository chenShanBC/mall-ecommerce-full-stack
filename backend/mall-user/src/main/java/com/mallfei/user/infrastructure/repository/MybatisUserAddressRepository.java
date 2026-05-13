package com.mallfei.user.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallfei.user.domain.model.UserAddress;
import com.mallfei.user.domain.repository.UserAddressRepository;
import com.mallfei.user.infrastructure.persistence.dataobject.UserAddressDO;
import com.mallfei.user.infrastructure.persistence.mapper.UserAddressMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class MybatisUserAddressRepository implements UserAddressRepository {

    private final UserAddressMapper userAddressMapper;

    public MybatisUserAddressRepository(UserAddressMapper userAddressMapper) {
        this.userAddressMapper = userAddressMapper;
    }

    @Override
    public List<UserAddress> findByUserId(Long userId) {
        return userAddressMapper.selectList(new LambdaQueryWrapper<UserAddressDO>()
                        .eq(UserAddressDO::getUserId, userId)
                        .isNull(UserAddressDO::getDeletedAt)
                        .orderByDesc(UserAddressDO::getIsDefault)
                        .orderByDesc(UserAddressDO::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Map<Long, List<UserAddress>> findByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userAddressMapper.selectList(new LambdaQueryWrapper<UserAddressDO>()
                        .in(UserAddressDO::getUserId, userIds)
                        .isNull(UserAddressDO::getDeletedAt)
                        .orderByDesc(UserAddressDO::getIsDefault)
                        .orderByDesc(UserAddressDO::getId))
                .stream()
                .map(this::toDomain)
                .collect(java.util.stream.Collectors.groupingBy(UserAddress::userId, LinkedHashMap::new, java.util.stream.Collectors.toList()));
    }

    @Override
    public Optional<UserAddress> findById(Long id) {
        UserAddressDO userAddressDO = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddressDO>()
                .eq(UserAddressDO::getId, id)
                .isNull(UserAddressDO::getDeletedAt)
                .last("limit 1"));
        return Optional.ofNullable(userAddressDO).map(this::toDomain);
    }

    @Override
    public UserAddress save(UserAddress userAddress) {
        UserAddressDO userAddressDO = toDataObject(userAddress);
        userAddressMapper.insert(userAddressDO);
        return toDomain(userAddressDO);
    }

    @Override
    public void update(UserAddress userAddress) {
        UserAddressDO userAddressDO = toDataObject(userAddress);
        userAddressMapper.updateById(userAddressDO);
    }

    @Override
    public void markDeleted(Long id) {
        UserAddressDO userAddressDO = new UserAddressDO();
        userAddressDO.setId(id);
        userAddressDO.setDeletedAt(LocalDateTime.now());
        userAddressDO.setIsDefault(0);
        userAddressMapper.updateById(userAddressDO);
    }

    @Override
    public void clearDefaultByUserId(Long userId) {
        List<UserAddressDO> addressDOS = userAddressMapper.selectList(new LambdaQueryWrapper<UserAddressDO>()
                .eq(UserAddressDO::getUserId, userId)
                .eq(UserAddressDO::getIsDefault, 1)
                .isNull(UserAddressDO::getDeletedAt));
        for (UserAddressDO addressDO : addressDOS) {
            addressDO.setIsDefault(0);
            userAddressMapper.updateById(addressDO);
        }
    }

    @Override
    public long countActiveByUserId(Long userId) {
        return userAddressMapper.selectCount(new LambdaQueryWrapper<UserAddressDO>()
                .eq(UserAddressDO::getUserId, userId)
                .isNull(UserAddressDO::getDeletedAt));
    }

    private UserAddressDO toDataObject(UserAddress userAddress) {
        UserAddressDO userAddressDO = new UserAddressDO();
        userAddressDO.setId(userAddress.id());
        userAddressDO.setUserId(userAddress.userId());
        userAddressDO.setReceiverName(userAddress.receiverName());
        userAddressDO.setReceiverPhone(userAddress.receiverPhone());
        userAddressDO.setProvinceCode(userAddress.provinceCode());
        userAddressDO.setProvinceName(userAddress.provinceName());
        userAddressDO.setCityCode(userAddress.cityCode());
        userAddressDO.setCityName(userAddress.cityName());
        userAddressDO.setDistrictCode(userAddress.districtCode());
        userAddressDO.setDistrictName(userAddress.districtName());
        userAddressDO.setDetailAddress(userAddress.detailAddress());
        userAddressDO.setPostalCode(userAddress.postalCode());
        userAddressDO.setIsDefault(userAddress.isDefault() ? 1 : 0);
        return userAddressDO;
    }

    private UserAddress toDomain(UserAddressDO userAddressDO) {
        return new UserAddress(
                userAddressDO.getId(),
                userAddressDO.getUserId(),
                userAddressDO.getReceiverName(),
                userAddressDO.getReceiverPhone(),
                userAddressDO.getProvinceCode(),
                userAddressDO.getProvinceName(),
                userAddressDO.getCityCode(),
                userAddressDO.getCityName(),
                userAddressDO.getDistrictCode(),
                userAddressDO.getDistrictName(),
                userAddressDO.getDetailAddress(),
                userAddressDO.getPostalCode(),
                userAddressDO.getIsDefault() != null && userAddressDO.getIsDefault() == 1
        );
    }
}
