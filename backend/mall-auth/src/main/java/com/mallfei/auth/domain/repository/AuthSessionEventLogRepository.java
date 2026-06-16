package com.mallfei.auth.domain.repository;

import com.mallfei.auth.domain.model.AuthSessionEventLog;

public interface AuthSessionEventLogRepository {

    AuthSessionEventLog save(AuthSessionEventLog log);
}
