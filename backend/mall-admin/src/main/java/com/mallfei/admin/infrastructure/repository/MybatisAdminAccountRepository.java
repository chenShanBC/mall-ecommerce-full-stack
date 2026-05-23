package com.mallfei.admin.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallfei.admin.domain.model.AdminAccount;
import com.mallfei.admin.domain.model.AdminPermissionCatalog;
import com.mallfei.admin.domain.repository.AdminAccountRepository;
import com.mallfei.admin.infrastructure.persistence.dataobject.AdminAccountDO;
import com.mallfei.admin.infrastructure.persistence.mapper.AdminAccountMapper;
import com.mallfei.common.api.PageResult;
import com.mallfei.common.exception.BusinessException;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class MybatisAdminAccountRepository implements AdminAccountRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final AdminAccountMapper adminAccountMapper;
    private final ObjectMapper objectMapper;

    public MybatisAdminAccountRepository(AdminAccountMapper adminAccountMapper,
                                         ObjectMapper objectMapper) {
        this.adminAccountMapper = adminAccountMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<AdminAccount> findByUsername(String username) {
        AdminAccountDO adminAccountDO = adminAccountMapper.selectOne(new LambdaQueryWrapper<AdminAccountDO>()
                .eq(AdminAccountDO::getUsername, username)
                .isNull(AdminAccountDO::getDeletedAt)
                .last("limit 1"));
        return Optional.ofNullable(adminAccountDO).map(this::toDomain);
    }

    @Override
    public Optional<AdminAccount> findById(Long id) {
        AdminAccountDO adminAccountDO = adminAccountMapper.selectOne(new LambdaQueryWrapper<AdminAccountDO>()
                .eq(AdminAccountDO::getId, id)
                .isNull(AdminAccountDO::getDeletedAt)
                .last("limit 1"));
        return Optional.ofNullable(adminAccountDO).map(this::toDomain);
    }

    @Override
    public List<AdminAccount> findAll() {
        return adminAccountMapper.selectList(new LambdaQueryWrapper<AdminAccountDO>()
                        .isNull(AdminAccountDO::getDeletedAt)
                        .orderByAsc(AdminAccountDO::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public PageResult<AdminAccount> search(String keyword, String roleCode, String status, long page, long size) {
        LambdaQueryWrapper<AdminAccountDO> wrapper = new LambdaQueryWrapper<AdminAccountDO>()
                .isNull(AdminAccountDO::getDeletedAt)
                .orderByAsc(AdminAccountDO::getId);
        if (keyword != null && !keyword.isBlank()) {
            String trimmed = keyword.trim();
            wrapper.and(w -> w.like(AdminAccountDO::getUsername, trimmed)
                    .or().like(AdminAccountDO::getNickname, trimmed)
                    .or().like(AdminAccountDO::getRoleCode, trimmed));
        }
        if (roleCode != null && !roleCode.isBlank()) {
            wrapper.eq(AdminAccountDO::getRoleCode, roleCode);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(AdminAccountDO::getStatus, status);
        }
        Page<AdminAccountDO> result = adminAccountMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)), wrapper);
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(), result.getRecords().stream().map(this::toDomain).toList());
    }

    @Override
    public boolean existsByUsername(String username) {
        Long count = adminAccountMapper.selectCount(new LambdaQueryWrapper<AdminAccountDO>()
                .eq(AdminAccountDO::getUsername, username)
                .isNull(AdminAccountDO::getDeletedAt));
        return count != null && count > 0;
    }

    @Override
    public AdminAccount save(AdminAccount adminAccount) {
        AdminAccountDO dataObject = new AdminAccountDO();
        fill(dataObject, adminAccount);
        adminAccountMapper.insert(dataObject);
        return findById(dataObject.getId()).orElseThrow();
    }

    @Override
    public AdminAccount update(AdminAccount adminAccount) {
        AdminAccountDO dataObject = new AdminAccountDO();
        dataObject.setId(adminAccount.id());
        fill(dataObject, adminAccount);
        adminAccountMapper.updateById(dataObject);
        return findById(adminAccount.id()).orElseThrow();
    }

    private void fill(AdminAccountDO dataObject, AdminAccount adminAccount) {
        dataObject.setUserId(adminAccount.userId());
        dataObject.setUsername(adminAccount.username());
        dataObject.setPasswordHash(adminAccount.passwordHash());
        dataObject.setNickname(adminAccount.nickname());
        dataObject.setRoleCode(adminAccount.roleCode());
        dataObject.setPermissionsJson(writePermissions(adminAccount.permissions()));
        dataObject.setStatus(adminAccount.status());
    }

    private AdminAccount toDomain(AdminAccountDO adminAccountDO) {
        String roleCode = AdminPermissionCatalog.normalizeRoleCode(adminAccountDO.getRoleCode());
        List<String> permissions = readPermissions(adminAccountDO.getPermissionsJson());
        if (permissions.isEmpty()) {
            permissions = AdminPermissionCatalog.defaultPermissions(roleCode);
        }
        return new AdminAccount(
                adminAccountDO.getId(),
                adminAccountDO.getUserId(),
                adminAccountDO.getUsername(),
                adminAccountDO.getPasswordHash(),
                adminAccountDO.getNickname(),
                roleCode,
                adminAccountDO.getStatus(),
                permissions
        );
    }

    private List<String> readPermissions(String permissionsJson) {
        if (permissionsJson == null || permissionsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(permissionsJson, STRING_LIST_TYPE);
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("管理员权限数据格式异常");
        }
    }

    private String writePermissions(List<String> permissions) {
        try {
            return objectMapper.writeValueAsString(permissions == null ? Collections.emptyList() : permissions);
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("管理员权限数据写入失败");
        }
    }
}
