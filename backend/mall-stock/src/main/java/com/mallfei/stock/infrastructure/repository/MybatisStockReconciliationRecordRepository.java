package com.mallfei.stock.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallfei.common.api.PageResult;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.stock.domain.model.StockConsistencySnapshot;
import com.mallfei.stock.domain.model.StockReconciliationRecord;
import com.mallfei.stock.domain.repository.StockReconciliationRecordRepository;
import com.mallfei.stock.infrastructure.persistence.dataobject.StockReconciliationRecordDO;
import com.mallfei.stock.infrastructure.persistence.mapper.StockReconciliationRecordMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisStockReconciliationRecordRepository implements StockReconciliationRecordRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final StockReconciliationRecordMapper mapper;
    private final ObjectMapper objectMapper;

    public MybatisStockReconciliationRecordRepository(StockReconciliationRecordMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public StockReconciliationRecord save(StockReconciliationRecord record) {
        StockReconciliationRecordDO dataObject = toDO(record);
        mapper.insert(dataObject);
        return toDomain(dataObject);
    }

    @Override
    public void update(StockReconciliationRecord record) {
        StockReconciliationRecordDO dataObject = toDO(record);
        dataObject.setId(record.id());
        int affected = mapper.updateById(dataObject);
        if (affected <= 0) throw BusinessException.badRequest("库存对账记录更新失败: " + record.id());
    }

    @Override
    public Optional<StockReconciliationRecord> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<StockReconciliationRecord> findLatestBySkuId(Long skuId) {
        if (skuId == null) return Optional.empty();
        return mapper.selectList(new LambdaQueryWrapper<StockReconciliationRecordDO>()
                        .eq(StockReconciliationRecordDO::getSkuId, skuId)
                        .orderByDesc(StockReconciliationRecordDO::getCheckedAt)
                        .orderByDesc(StockReconciliationRecordDO::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    public boolean existsPendingInconsistent(Long skuId) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<StockReconciliationRecordDO>()
                .eq(StockReconciliationRecordDO::getSkuId, skuId)
                .eq(StockReconciliationRecordDO::getStatus, StockReconciliationRecord.STATUS_INCONSISTENT)
                .eq(StockReconciliationRecordDO::getRepairStatus, StockReconciliationRecord.REPAIR_PENDING));
        return count != null && count > 0;
    }

    @Override
    public PageResult<StockReconciliationRecord> page(Long skuId, String status, long page, long size, String sortBy, String sortOrder) {
        LambdaQueryWrapper<StockReconciliationRecordDO> wrapper = new LambdaQueryWrapper<>();
        if (skuId != null) wrapper.eq(StockReconciliationRecordDO::getSkuId, skuId);
        if (status != null && !status.isBlank()) wrapper.eq(StockReconciliationRecordDO::getStatus, status);
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if ("checkedAt".equalsIgnoreCase(sortBy)) {
            wrapper.orderBy(true, asc, StockReconciliationRecordDO::getCheckedAt).orderByDesc(StockReconciliationRecordDO::getId);
        } else {
            wrapper.orderBy(true, asc, StockReconciliationRecordDO::getId);
        }
        Page<StockReconciliationRecordDO> result = mapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)), wrapper);
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(), result.getRecords().stream().map(this::toDomain).toList());
    }

    private StockReconciliationRecordDO toDO(StockReconciliationRecord record) {
        StockReconciliationRecordDO dataObject = new StockReconciliationRecordDO();
        dataObject.setId(record.id());
        dataObject.setSkuId(record.skuId());
        dataObject.setStatus(record.status());
        dataObject.setSeverity(record.severity());
        dataObject.setStockSnapshotJson(writeJson(record.stockSnapshot()));
        dataObject.setExpectedSnapshotJson(writeJson(record.expectedSnapshot()));
        dataObject.setRedisSnapshotJson(writeJson(record.redisSnapshot()));
        dataObject.setDifferencesJson(writeJson(record.differences()));
        dataObject.setRepairStatus(record.repairStatus());
        dataObject.setRepairRemark(record.repairRemark());
        dataObject.setCheckedAt(record.checkedAt());
        dataObject.setRepairedAt(record.repairedAt());
        dataObject.setCreatedAt(record.createdAt());
        dataObject.setUpdatedAt(record.updatedAt());
        return dataObject;
    }

    private StockReconciliationRecord toDomain(StockReconciliationRecordDO dataObject) {
        return new StockReconciliationRecord(
                dataObject.getId(),
                dataObject.getSkuId(),
                dataObject.getStatus(),
                dataObject.getSeverity(),
                readSnapshot(dataObject.getStockSnapshotJson()),
                readSnapshot(dataObject.getExpectedSnapshotJson()),
                readSnapshot(dataObject.getRedisSnapshotJson()),
                readStringList(dataObject.getDifferencesJson()),
                dataObject.getRepairStatus(),
                dataObject.getRepairRemark(),
                dataObject.getCheckedAt(),
                dataObject.getRepairedAt(),
                dataObject.getCreatedAt(),
                dataObject.getUpdatedAt()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw BusinessException.badRequest("库存对账记录序列化失败: " + e.getMessage());
        }
    }

    private StockConsistencySnapshot readSnapshot(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) return null;
        try {
            return objectMapper.readValue(json, StockConsistencySnapshot.class);
        } catch (Exception e) {
            throw BusinessException.badRequest("库存对账快照反序列化失败: " + e.getMessage());
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) return List.of();
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception e) {
            throw BusinessException.badRequest("库存对账差异反序列化失败: " + e.getMessage());
        }
    }
}
