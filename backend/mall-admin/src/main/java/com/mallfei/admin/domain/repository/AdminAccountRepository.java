package com.mallfei.admin.domain.repository;

import com.mallfei.admin.domain.model.AdminAccount;
import com.mallfei.common.api.PageResult;

import java.util.List;
import java.util.Optional;

public interface AdminAccountRepository {

    Optional<AdminAccount> findByUsername(String username);

    Optional<AdminAccount> findById(Long id);

    List<AdminAccount> findAll();

    PageResult<AdminAccount> search(String keyword, String roleCode, String status, long page, long size);

    boolean existsByUsername(String username);

    AdminAccount save(AdminAccount adminAccount);

    AdminAccount update(AdminAccount adminAccount);
}
