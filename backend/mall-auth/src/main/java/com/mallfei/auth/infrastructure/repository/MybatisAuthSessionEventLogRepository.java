package com.mallfei.auth.infrastructure.repository;

import com.mallfei.auth.domain.model.AuthDeviceType;
import com.mallfei.auth.domain.model.AuthSessionEventLog;
import com.mallfei.auth.domain.model.AuthSessionEventType;
import com.mallfei.auth.domain.repository.AuthSessionEventLogRepository;
import com.mallfei.auth.infrastructure.persistence.dataobject.AuthSessionEventLogDO;
import com.mallfei.auth.infrastructure.persistence.mapper.AuthSessionEventLogMapper;
import com.mallfei.common.enums.IdentityType;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisAuthSessionEventLogRepository implements AuthSessionEventLogRepository {

    private final AuthSessionEventLogMapper authSessionEventLogMapper;

    public MybatisAuthSessionEventLogRepository(AuthSessionEventLogMapper authSessionEventLogMapper) {
        this.authSessionEventLogMapper = authSessionEventLogMapper;
    }

    @Override
    public AuthSessionEventLog save(AuthSessionEventLog log) {
        AuthSessionEventLogDO dataObject = toDataObject(log);
        authSessionEventLogMapper.insert(dataObject);
        return toDomain(dataObject);
    }

    private AuthSessionEventLogDO toDataObject(AuthSessionEventLog log) {
        AuthSessionEventLogDO dataObject = new AuthSessionEventLogDO();
        dataObject.setPrincipalId(log.principalId());
        dataObject.setIdentityType(log.identityType().code());
        dataObject.setAccount(log.account());
        dataObject.setDeviceType(log.deviceType().code());
        dataObject.setEventType(log.eventType().name());
        dataObject.setResult(log.result());
        dataObject.setLoginId(log.loginId());
        dataObject.setTokenDigest(log.tokenDigest());
        dataObject.setIp(log.ip());
        dataObject.setUserAgent(log.userAgent());
        dataObject.setMessage(log.message());
        dataObject.setCreatedAt(log.createdAt());
        return dataObject;
    }

    private AuthSessionEventLog toDomain(AuthSessionEventLogDO dataObject) {
        return new AuthSessionEventLog(
                dataObject.getId(),
                dataObject.getPrincipalId(),
                IdentityType.fromCode(dataObject.getIdentityType()),
                dataObject.getAccount(),
                AuthDeviceType.fromNullable(dataObject.getDeviceType(), IdentityType.fromCode(dataObject.getIdentityType())),
                AuthSessionEventType.valueOf(dataObject.getEventType()),
                dataObject.getResult(),
                dataObject.getLoginId(),
                dataObject.getTokenDigest(),
                dataObject.getIp(),
                dataObject.getUserAgent(),
                dataObject.getMessage(),
                dataObject.getCreatedAt()
        );
    }
}
