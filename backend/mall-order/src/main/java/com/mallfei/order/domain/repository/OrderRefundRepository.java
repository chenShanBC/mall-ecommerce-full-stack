package com.mallfei.order.domain.repository;

import com.mallfei.order.domain.model.OrderRefund;

import java.util.Optional;

public interface OrderRefundRepository {

    OrderRefund save(OrderRefund refund);

    OrderRefund update(OrderRefund refund);

    Optional<OrderRefund> findLatestByOrderNo(String orderNo);
}
