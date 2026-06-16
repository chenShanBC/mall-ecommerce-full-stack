package com.mallfei.auth.domain.service;

import com.mallfei.auth.domain.model.AuthSessionEventLog;
import com.mallfei.auth.domain.repository.AuthSessionEventLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionEventLogService {

    private static final Logger log = LoggerFactory.getLogger(AuthSessionEventLogService.class);

    private final AuthSessionEventLogRepository authSessionEventLogRepository;

    public AuthSessionEventLogService(AuthSessionEventLogRepository authSessionEventLogRepository) {
        this.authSessionEventLogRepository = authSessionEventLogRepository;
    }

    public void appendSafely(AuthSessionEventLog eventLog) {
        try {
            authSessionEventLogRepository.save(eventLog);
        } catch (Exception exception) {
            log.warn("Failed to append auth session event log, eventType={}, identityType={}, principalId={}",
                    eventLog.eventType(), eventLog.identityType().code(), eventLog.principalId(), exception);
        }
    }
}
