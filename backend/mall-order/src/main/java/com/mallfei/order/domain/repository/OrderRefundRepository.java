package com.mallfei.order.domain.repository;

import com.mallfei.order.domain.model.OrderRefund;

import java.util.List;
import java.util.Optional;

public interface OrderRefundRepository {

    OrderRefund save(OrderRefund refund);

    OrderRefund update(OrderRefund refund);

    Optional<OrderRefund> findLatestByOrderNo(String orderNo);

    Optional<OrderRefund> findByRefundNo(String refundNo);

    List<OrderRefund> findByOrderNo(String orderNo);

    List<OrderRefund> search(String status, String keyword);

    OrderRefund submitForRefunding(String refundNo);
}
