package com.mallfei.pay.domain.repository;

import com.mallfei.pay.domain.model.PayRefundOrder;

import java.util.Optional;

public interface PayRefundOrderRepository {

    PayRefundOrder save(PayRefundOrder payRefundOrder);

    void update(PayRefundOrder payRefundOrder);

    Optional<PayRefundOrder> findByRefundNo(String refundNo);
}
