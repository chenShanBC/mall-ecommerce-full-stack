package com.mallfei.aftersale.domain.repository;

import com.mallfei.aftersale.domain.model.AftersaleOrder;

import java.util.List;
import java.util.Optional;

public interface AftersaleOrderRepository {

    AftersaleOrder save(AftersaleOrder aftersaleOrder);

    Optional<AftersaleOrder> findByAftersaleNo(String aftersaleNo);

    Optional<AftersaleOrder> findLatestByOrderNo(String orderNo);

    List<AftersaleOrder> findAll();

    void update(AftersaleOrder aftersaleOrder);
}
