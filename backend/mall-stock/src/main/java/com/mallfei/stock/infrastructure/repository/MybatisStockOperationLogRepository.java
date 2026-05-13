package com.mallfei.stock.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallfei.common.api.PageResult;
import com.mallfei.stock.domain.model.StockOperationLog;
import com.mallfei.stock.domain.repository.StockOperationLogRepository;
import com.mallfei.stock.infrastructure.persistence.dataobject.StockOperationLogDO;
import com.mallfei.stock.infrastructure.persistence.mapper.StockOperationLogMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class MybatisStockOperationLogRepository implements StockOperationLogRepository {

    private final StockOperationLogMapper stockOperationLogMapper;

    public MybatisStockOperationLogRepository(StockOperationLogMapper stockOperationLogMapper) {
        this.stockOperationLogMapper = stockOperationLogMapper;
    }

    @Override
    public StockOperationLog save(StockOperationLog stockOperationLog) {
        StockOperationLogDO logDO = toDO(stockOperationLog);
        stockOperationLogMapper.insert(logDO);
        return toDomain(logDO);
    }

    @Override
    public PageResult<StockOperationLog> page(Long skuId,
                                              String operationType,
                                              LocalDateTime startTime,
                                              LocalDateTime endTime,
                                              long page,
                                              long size) {
        LambdaQueryWrapper<StockOperationLogDO> wrapper = new LambdaQueryWrapper<>();
        if (skuId != null) {
            wrapper.eq(StockOperationLogDO::getSkuId, skuId);
        }
        if (operationType != null && !operationType.isBlank()) {
            wrapper.eq(StockOperationLogDO::getOperationType, operationType);
        }
        if (startTime != null) {
            wrapper.ge(StockOperationLogDO::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(StockOperationLogDO::getCreatedAt, endTime);
        }
        List<StockOperationLog> logs = stockOperationLogMapper.selectList(wrapper.orderByDesc(StockOperationLogDO::getId))
                .stream()
                .map(this::toDomain)
                .toList();
        return PageResult.of(logs, page, size);
    }

    private StockOperationLogDO toDO(StockOperationLog stockOperationLog) {
        StockOperationLogDO logDO = new StockOperationLogDO();
        logDO.setId(stockOperationLog.id());
        logDO.setSkuId(stockOperationLog.skuId());
        logDO.setOperationType(stockOperationLog.operationType());
        logDO.setBusinessType(stockOperationLog.businessType());
        logDO.setBusinessNo(stockOperationLog.businessNo());
        logDO.setChangeQuantity(stockOperationLog.changeQuantity());
        logDO.setBeforeTotalStock(stockOperationLog.beforeTotalStock());
        logDO.setBeforeLockedStock(stockOperationLog.beforeLockedStock());
        logDO.setBeforeAvailableStock(stockOperationLog.beforeAvailableStock());
        logDO.setAfterTotalStock(stockOperationLog.afterTotalStock());
        logDO.setAfterLockedStock(stockOperationLog.afterLockedStock());
        logDO.setAfterAvailableStock(stockOperationLog.afterAvailableStock());
        logDO.setRemark(stockOperationLog.remark());
        logDO.setOperatorType(stockOperationLog.operatorType());
        logDO.setOperatorId(stockOperationLog.operatorId());
        logDO.setOperatorName(stockOperationLog.operatorName());
        logDO.setSourceType(stockOperationLog.sourceType());
        logDO.setCreatedAt(stockOperationLog.createdAt());
        return logDO;
    }

    private StockOperationLog toDomain(StockOperationLogDO logDO) {
        return new StockOperationLog(
                logDO.getId(),
                logDO.getSkuId(),
                logDO.getOperationType(),
                logDO.getBusinessType(),
                logDO.getBusinessNo(),
                logDO.getChangeQuantity(),
                logDO.getBeforeTotalStock(),
                logDO.getBeforeLockedStock(),
                logDO.getBeforeAvailableStock(),
                logDO.getAfterTotalStock(),
                logDO.getAfterLockedStock(),
                logDO.getAfterAvailableStock(),
                logDO.getRemark(),
                logDO.getOperatorType(),
                logDO.getOperatorId(),
                logDO.getOperatorName(),
                logDO.getSourceType(),
                logDO.getCreatedAt()
        );
    }
}
