package com.mallfei.stock.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mallfei.stock.domain.model.StockLockRecord;
import com.mallfei.stock.domain.repository.StockLockRepository;
import com.mallfei.stock.infrastructure.persistence.dataobject.StockLockRecordDO;
import com.mallfei.stock.infrastructure.persistence.mapper.StockLockRecordMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisStockLockRepository implements StockLockRepository {

    private final StockLockRecordMapper stockLockRecordMapper;

    public MybatisStockLockRepository(StockLockRecordMapper stockLockRecordMapper) {
        this.stockLockRecordMapper = stockLockRecordMapper;
    }

    @Override
    public Optional<StockLockRecord> findByBusiness(String businessType, String businessNo, Long skuId) {
        StockLockRecordDO recordDO = stockLockRecordMapper.selectOne(new LambdaQueryWrapper<StockLockRecordDO>()
                .eq(StockLockRecordDO::getBusinessType, businessType)
                .eq(StockLockRecordDO::getBusinessNo, businessNo)
                .eq(StockLockRecordDO::getSkuId, skuId)
                .last("limit 1"));
        return Optional.ofNullable(recordDO).map(this::toDomain);
    }

    @Override
    public StockLockRecord save(StockLockRecord stockLockRecord) {
        StockLockRecordDO recordDO = toDO(stockLockRecord);
        stockLockRecordMapper.insert(recordDO);
        return toDomain(recordDO);
    }

    @Override
    public void update(StockLockRecord stockLockRecord) {
        StockLockRecordDO recordDO = toDO(stockLockRecord);
        recordDO.setId(stockLockRecord.id());
        stockLockRecordMapper.updateById(recordDO);
    }

    @Override
    public List<StockLockRecord> findByBusiness(String businessType, String businessNo) {
        return stockLockRecordMapper.selectList(new LambdaQueryWrapper<StockLockRecordDO>()
                        .eq(StockLockRecordDO::getBusinessType, businessType)
                        .eq(StockLockRecordDO::getBusinessNo, businessNo)
                        .orderByAsc(StockLockRecordDO::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<StockLockRecord> findExpiredReserved(int minutes) {
        return stockLockRecordMapper.selectList(new LambdaQueryWrapper<StockLockRecordDO>()
                        .eq(StockLockRecordDO::getStatus, StockLockRecord.STATUS_RESERVED)
                        .le(StockLockRecordDO::getLockTime, java.time.LocalDateTime.now().minusMinutes(minutes))
                        .orderByAsc(StockLockRecordDO::getId)
                        .last("limit 200"))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<StockLockRecord> findUnpersistedReservations(int minutes, int limit) {
        return stockLockRecordMapper.selectList(new LambdaQueryWrapper<StockLockRecordDO>()
                        .eq(StockLockRecordDO::getStatus, StockLockRecord.STATUS_RESERVED)
                        .eq(StockLockRecordDO::getReservedSynced, false)
                        .le(StockLockRecordDO::getLockTime, java.time.LocalDateTime.now().minusMinutes(minutes))
                        .orderByAsc(StockLockRecordDO::getId)
                        .last("limit " + Math.max(1, limit)))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long sumReservedQuantity(Long skuId) {
        Object result = stockLockRecordMapper.selectObjs(Wrappers.<StockLockRecordDO>query()
                .select("COALESCE(SUM(quantity), 0)")
                .eq("sku_id", skuId)
                .eq("status", StockLockRecord.STATUS_RESERVED))
                .stream()
                .findFirst()
                .orElse(0);
        return Long.parseLong(String.valueOf(result));
    }

    @Override
    public long sumUnpersistedReservedQuantity(Long skuId) {
        Object result = stockLockRecordMapper.selectObjs(Wrappers.<StockLockRecordDO>query()
                .select("COALESCE(SUM(quantity), 0)")
                .eq("sku_id", skuId)
                .eq("status", StockLockRecord.STATUS_RESERVED)
                .eq("reserved_synced", 0))
                .stream()
                .findFirst()
                .orElse(0);
        return Long.parseLong(String.valueOf(result));
    }

    private StockLockRecordDO toDO(StockLockRecord stockLockRecord) {
        StockLockRecordDO recordDO = new StockLockRecordDO();
        recordDO.setId(stockLockRecord.id());
        recordDO.setLockNo(stockLockRecord.lockNo());
        recordDO.setSkuId(stockLockRecord.skuId());
        recordDO.setBusinessType(stockLockRecord.businessType());
        recordDO.setBusinessNo(stockLockRecord.businessNo());
        recordDO.setQuantity(stockLockRecord.quantity());
        recordDO.setStatus(stockLockRecord.status());
        recordDO.setLockTime(stockLockRecord.lockTime());
        recordDO.setReleaseTime(stockLockRecord.releaseTime());
        recordDO.setDeductTime(stockLockRecord.deductTime());
        recordDO.setReservedSynced(stockLockRecord.reservedSyncedSafe());
        recordDO.setCancelledSynced(stockLockRecord.cancelledSyncedSafe());
        recordDO.setConfirmedSynced(stockLockRecord.confirmedSyncedSafe());
        return recordDO;
    }

    private StockLockRecord toDomain(StockLockRecordDO recordDO) {
        return new StockLockRecord(
                recordDO.getId(),
                recordDO.getLockNo(),
                recordDO.getSkuId(),
                recordDO.getBusinessType(),
                recordDO.getBusinessNo(),
                recordDO.getQuantity(),
                recordDO.getStatus(),
                recordDO.getLockTime(),
                recordDO.getReleaseTime(),
                recordDO.getDeductTime(),
                recordDO.getReservedSynced(),
                recordDO.getCancelledSynced(),
                recordDO.getConfirmedSynced()
        );
    }
}
