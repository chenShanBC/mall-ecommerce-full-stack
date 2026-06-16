package com.mallfei.order.domain.repository;

import com.mallfei.order.domain.model.OrderRefundItem;

import java.util.List;

public interface OrderRefundItemRepository {

    void saveBatch(List<OrderRefundItem> items);

    List<OrderRefundItem> findByRefundNo(String refundNo);
}
