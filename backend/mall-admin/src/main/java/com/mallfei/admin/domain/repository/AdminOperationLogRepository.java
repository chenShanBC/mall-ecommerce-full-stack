package com.mallfei.admin.domain.repository;

import com.mallfei.admin.domain.model.AdminOperationLog;

import java.util.List;

public interface AdminOperationLogRepository {

    AdminOperationLog save(AdminOperationLog log);

    List<AdminOperationLog> findAll();
}
