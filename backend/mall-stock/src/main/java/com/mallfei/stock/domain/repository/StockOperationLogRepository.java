package com.mallfei.stock.domain.repository;

import com.mallfei.common.api.PageResult;
import com.mallfei.stock.domain.model.StockOperationLog;

import java.time.LocalDateTime;

public interface StockOperationLogRepository {

    boolean exists(Long skuId, String operationType, String businessType, String businessNo);

    StockOperationLog save(StockOperationLog stockOperationLog);

    boolean saveIfAbsent(StockOperationLog stockOperationLog);

    PageResult<StockOperationLog> page(Long skuId,
                                       String operationType,
                                       LocalDateTime startTime,
                                       LocalDateTime endTime,
                                       long page,
                                       long size);
}
