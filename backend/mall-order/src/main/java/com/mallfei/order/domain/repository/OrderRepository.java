package com.mallfei.order.domain.repository;

import com.mallfei.common.api.PageResult;
import com.mallfei.order.domain.model.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    Optional<Order> findByOrderNo(String orderNo);

    List<Order> findByUserId(Long userId);

    List<Order> findAll();

    PageResult<Order> search(String status, String keyword, long page, long size);

    PageResult<Order> search(String status, String keyword, long page, long size, String sortBy, String sortOrder);

    long countAll();

    long countByStatus(String status);

    long countCancelled();

    long sumPaidAmount();

    void update(Order order);

    void markUserDeleted(Long orderId, Long userId);
}
