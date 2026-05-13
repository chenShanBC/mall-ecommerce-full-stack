package com.mallfei.admin.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallfei.admin.domain.model.AdminOperationLog;
import com.mallfei.admin.domain.repository.AdminOperationLogRepository;
import com.mallfei.admin.infrastructure.persistence.dataobject.AdminOperationLogDO;
import com.mallfei.admin.infrastructure.persistence.mapper.AdminOperationLogMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisAdminOperationLogRepository implements AdminOperationLogRepository {

    private final AdminOperationLogMapper adminOperationLogMapper;

    public MybatisAdminOperationLogRepository(AdminOperationLogMapper adminOperationLogMapper) {
        this.adminOperationLogMapper = adminOperationLogMapper;
    }

    @Override
    public AdminOperationLog save(AdminOperationLog log) {
        AdminOperationLogDO dataObject = new AdminOperationLogDO();
        dataObject.setOperatorAdminId(log.operatorAdminId());
        dataObject.setOperatorUsername(log.operatorUsername());
        dataObject.setOperationModule(log.operationModule());
        dataObject.setOperationType(log.operationType());
        dataObject.setOperationContent(log.operationContent());
        dataObject.setOperationResult(log.operationResult());
        adminOperationLogMapper.insert(dataObject);
        return new AdminOperationLog(
                dataObject.getId(),
                dataObject.getOperatorAdminId(),
                dataObject.getOperatorUsername(),
                dataObject.getOperationModule(),
                dataObject.getOperationType(),
                dataObject.getOperationContent(),
                dataObject.getOperationResult(),
                dataObject.getCreatedAt()
        );
    }

    @Override
    public List<AdminOperationLog> findAll() {
        return adminOperationLogMapper.selectList(new LambdaQueryWrapper<AdminOperationLogDO>()
                        .orderByDesc(AdminOperationLogDO::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private AdminOperationLog toDomain(AdminOperationLogDO dataObject) {
        return new AdminOperationLog(
                dataObject.getId(),
                dataObject.getOperatorAdminId(),
                dataObject.getOperatorUsername(),
                dataObject.getOperationModule(),
                dataObject.getOperationType(),
                dataObject.getOperationContent(),
                dataObject.getOperationResult(),
                dataObject.getCreatedAt()
        );
    }
}
