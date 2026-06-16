package com.mallfei.stock.application.service;

import com.mallfei.common.api.PageResult;
import com.mallfei.stock.domain.model.Stock;
import com.mallfei.stock.domain.model.StockOperationLog;
import com.mallfei.stock.domain.repository.StockOperationLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class StockOperationLogApplicationService {

    private final StockOperationLogRepository stockOperationLogRepository;

    public StockOperationLogApplicationService(StockOperationLogRepository stockOperationLogRepository) {
        this.stockOperationLogRepository = stockOperationLogRepository;
    }

    public void record(Long skuId,
                       String operationType,
                       String businessType,
                       String businessNo,
                       Integer changeQuantity,
                       Stock before,
                       Stock after,
                       String remark,
                       String sourceType) {
        recordIfAbsent(skuId, operationType, businessType, businessNo, changeQuantity, before, after, remark, sourceType);
    }

    public boolean recordIfAbsent(Long skuId,
                                  String operationType,
                                  String businessType,
                                  String businessNo,
                                  Integer changeQuantity,
                                  Stock before,
                                  Stock after,
                                  String remark,
                                  String sourceType) {
        String operatorType = "SYSTEM";
        String operatorName = "system";
        if ("ADMIN".equalsIgnoreCase(businessType) || "ADMIN_UI".equalsIgnoreCase(sourceType)) {
            operatorType = "ADMIN";
            operatorName = "admin";
        }
        return stockOperationLogRepository.saveIfAbsent(StockOperationLog.of(
                skuId,
                operationType,
                businessType,
                businessNo,
                changeQuantity,
                before,
                after,
                remark,
                operatorType,
                null,
                operatorName,
                sourceType,
                LocalDateTime.now()
        ));
    }

    public PageResult<StockOperationLog> pageLogs(Long skuId,
                                                  String operationType,
                                                  LocalDateTime startTime,
                                                  LocalDateTime endTime,
                                                  long page,
                                                  long size) {
        return stockOperationLogRepository.page(skuId, operationType, startTime, endTime, page, size);
    }
}
