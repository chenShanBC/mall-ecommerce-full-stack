package com.mallfei.pay.domain.repository;

import com.mallfei.common.api.PageResult;
import com.mallfei.pay.domain.model.PayOrder;

import java.util.List;
import java.util.Optional;

public interface PayOrderRepository {

    PayOrder save(PayOrder payOrder);

    Optional<PayOrder> findByPayOrderNo(String payOrderNo);

    Optional<PayOrder> findByOrderNo(String orderNo);

    List<PayOrder> findByOrderNoList(String orderNo);

    List<PayOrder> findPendingByOrderNo(String orderNo);

    List<PayOrder> findAll();

    PageResult<PayOrder> search(String status, String keyword, long page, long size);

    PageResult<PayOrder> search(String status, String keyword, long page, long size, String sortBy, String sortOrder);

    PageResult<PayOrder> search(String status, String keyword, java.time.LocalDate startDate, java.time.LocalDate endDate, long page, long size, String sortBy, String sortOrder);

    long countAll();

    long countPending();

    long countSuccess();

    long countClosed();

    void update(PayOrder payOrder);
}
