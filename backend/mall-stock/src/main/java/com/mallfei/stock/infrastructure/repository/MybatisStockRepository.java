package com.mallfei.stock.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallfei.common.api.PageResult;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.stock.application.dto.StockQuery;
import com.mallfei.stock.domain.model.Stock;
import com.mallfei.stock.domain.repository.StockRepository;
import com.mallfei.stock.infrastructure.persistence.dataobject.StockDO;
import com.mallfei.stock.infrastructure.persistence.mapper.StockMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisStockRepository implements StockRepository {

    private final StockMapper stockMapper;

    public MybatisStockRepository(StockMapper stockMapper) {
        this.stockMapper = stockMapper;
    }

    @Override
    public Optional<Stock> findBySkuId(Long skuId) {
        StockDO stockDO = stockMapper.selectOne(new LambdaQueryWrapper<StockDO>().eq(StockDO::getSkuId, skuId).last("limit 1"));
        return Optional.ofNullable(stockDO).map(this::toDomain);
    }

    @Override
    public List<Stock> findBySkuIds(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) return List.of();
        return stockMapper.selectList(new LambdaQueryWrapper<StockDO>().in(StockDO::getSkuId, skuIds).orderByAsc(StockDO::getSkuId)).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Stock> findAll() {
        return stockMapper.selectList(new LambdaQueryWrapper<StockDO>().orderByAsc(StockDO::getSkuId)).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Stock> search(StockQuery query) {
        return stockMapper.selectList(applySort(buildQueryWrapper(query), query)).stream().map(this::toDomain).toList();
    }

    @Override
    public PageResult<Stock> searchPage(StockQuery query) {
        Page<StockDO> result = stockMapper.selectPage(new Page<>(query == null ? 1 : Math.max(query.page(), 1), query == null ? 20 : Math.max(query.size(), 1)), applySort(buildQueryWrapper(query), query));
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(), result.getRecords().stream().map(this::toDomain).toList());
    }

    @Override
    public long countByWarningStatus(String warningStatus) {
        if (warningStatus == null || warningStatus.isBlank()) return stockMapper.selectCount(null);
        return stockMapper.selectCount(new LambdaQueryWrapper<StockDO>().eq(StockDO::getWarningStatus, warningStatus));
    }

    @Override
    public Stock save(Stock stock) {
        StockDO stockDO = toDO(stock);
        stockMapper.insert(stockDO);
        return toDomain(stockDO);
    }

    @Override
    public void update(Stock stock) {
        StockDO stockDO = toDO(stock);
        stockDO.setId(stock.id());
        int affected = stockMapper.updateById(stockDO);
        if (affected <= 0) throw BusinessException.badRequest("库存更新失败，未匹配到可更新记录: sku=" + stock.skuId());
    }

    private LambdaQueryWrapper<StockDO> buildQueryWrapper(StockQuery query) {
        LambdaQueryWrapper<StockDO> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.skuId() != null) wrapper.eq(StockDO::getSkuId, query.skuId());
            if (query.stockStatus() != null && !query.stockStatus().isBlank()) wrapper.eq(StockDO::getStockStatus, query.stockStatus());
            if (query.warningStatus() != null && !query.warningStatus().isBlank()) wrapper.eq(StockDO::getWarningStatus, query.warningStatus());
        }
        return wrapper;
    }

    private LambdaQueryWrapper<StockDO> applySort(LambdaQueryWrapper<StockDO> wrapper, StockQuery query) {
        String sortBy = query == null ? null : query.sortBy();
        String sortOrder = query == null ? null : query.sortOrder();
        boolean asc = !"desc".equalsIgnoreCase(sortOrder);
        if (sortBy == null || sortBy.isBlank()) {
            return wrapper.orderByAsc(StockDO::getSkuId);
        }
        return switch (sortBy) {
            case "skuId" -> wrapper.orderBy(true, asc, StockDO::getSkuId);
            case "availableStock" -> wrapper.orderBy(true, asc, StockDO::getAvailableStock);
            case "lockedStock" -> wrapper.orderBy(true, asc, StockDO::getLockedStock);
            case "totalStock" -> wrapper.orderBy(true, asc, StockDO::getTotalStock);
            case "stockStatus" -> wrapper.orderBy(true, asc, StockDO::getStockStatus);
            case "warningStatus" -> wrapper.orderBy(true, asc, StockDO::getWarningStatus);
            case "lowStockThreshold" -> wrapper.orderBy(true, asc, StockDO::getLowStockThreshold);
            case "highStockThreshold" -> wrapper.orderBy(true, asc, StockDO::getHighStockThreshold);
            default -> wrapper.orderByAsc(StockDO::getSkuId);
        };
    }

    private StockDO toDO(Stock stock) {
        StockDO stockDO = new StockDO();
        stockDO.setId(stock.id());
        stockDO.setSkuId(stock.skuId());
        stockDO.setTotalStock(stock.totalStock());
        stockDO.setLockedStock(stock.lockedStock());
        stockDO.setAvailableStock(stock.availableStock());
        stockDO.setStockStatus(stock.stockStatus());
        stockDO.setLowStockThreshold(stock.lowStockThreshold());
        stockDO.setHighStockThreshold(stock.highStockThreshold());
        stockDO.setWarningStatus(stock.warningStatus());
        stockDO.setVersion(stock.version());
        return stockDO;
    }

    private Stock toDomain(StockDO stockDO) {
        return new Stock(stockDO.getId(), stockDO.getSkuId(), stockDO.getTotalStock(), stockDO.getLockedStock(), stockDO.getAvailableStock(), stockDO.getStockStatus(), stockDO.getLowStockThreshold(), stockDO.getHighStockThreshold(), stockDO.getWarningStatus(), stockDO.getVersion());
    }
}
