package com.mallfei.order.domain.repository;

import com.mallfei.common.api.PageResult;
import com.mallfei.order.domain.model.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    Optional<Order> findByOrderNo(String orderNo);

    List<Order> findByUserId(Long userId);

    PageResult<Order> pageByUserId(Long userId, String status, String keyword, LocalDateTime createdAfter, long page, long size);

    List<Order> findAll();

    PageResult<Order> search(String status, String keyword, long page, long size);

    PageResult<Order> search(String status, String keyword, java.time.LocalDate startDate, java.time.LocalDate endDate, long page, long size, String sortBy, String sortOrder);

    PageResult<Order> search(String status, String keyword, long page, long size, String sortBy, String sortOrder);

    long countAll();

    long countByStatus(String status);

    long countCancelled();

    long sumPaidAmount();

    void update(Order order);

    boolean reviseReceiver(Order order);

    boolean updateReceiverAddress(String orderNo, String receiverName, String receiverPhone, String receiverProvinceName, String receiverCityName, String receiverDistrictName, String receiverDetailAddress, String note);

    int markPaymentException(String orderNo, String note);

    boolean restorePendingPayment(String orderNo, String note);

    boolean markPaid(String orderNo, LocalDateTime paidAt);

    boolean markPaidByAdmin(String orderNo, LocalDateTime paidAt, String note);

    void replaceOrderItem(Order order, OrderItemReplacement replacement);

    boolean closeTimedOut(String orderNo, LocalDateTime now);

    record OrderItemReplacement(Long orderItemId, Long targetSkuId, Long targetSpuId, String targetSkuName, String targetSkuImageUrl, Long targetSalePriceCent, String remark) {
    }

    List<Order> findTimedOutPendingOrders(LocalDateTime now, int limit);

    List<Order> findTimedOutPendingOrdersByUserId(Long userId, LocalDateTime now, int limit);

    void markUserDeleted(Long orderId, Long userId);
}
