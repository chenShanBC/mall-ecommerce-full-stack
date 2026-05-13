package com.mallfei.stock.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
                recordDO.getDeductTime()
        );
    }
}
