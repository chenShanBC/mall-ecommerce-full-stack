package com.mallfei.stock.domain.repository;

import com.mallfei.common.api.PageResult;
import com.mallfei.stock.application.dto.StockQuery;
import com.mallfei.stock.domain.model.Stock;

import java.util.List;
import java.util.Optional;

public interface StockRepository {

    Optional<Stock> findBySkuId(Long skuId);

    List<Stock> findBySkuIds(List<Long> skuIds);

    List<Stock> findAll();

    List<Stock> search(StockQuery query);

    PageResult<Stock> searchPage(StockQuery query);

    long countByWarningStatus(String warningStatus);

    Stock save(Stock stock);

    void update(Stock stock);
}
