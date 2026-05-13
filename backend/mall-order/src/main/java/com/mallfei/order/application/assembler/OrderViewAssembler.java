package com.mallfei.order.application.assembler;

import com.mallfei.order.application.vo.OrderDetailView;
import com.mallfei.order.application.vo.OrderItemView;
import com.mallfei.order.application.vo.OrderPaidPreviewView;
import com.mallfei.order.application.vo.OrderSummaryView;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.domain.model.OrderItem;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class OrderViewAssembler {

    public OrderSummaryView toSummary(Order order, long timeoutMinutes) {
        OrderItem firstItem = order.items().isEmpty() ? null : order.items().get(0);
        return new OrderSummaryView(
                order.id(),
                order.orderNo(),
                order.orderStatus(),
                order.totalAmountCent(),
                order.payAmountCent(),
                order.freightAmountCent(),
                order.discountAmountCent(),
                order.payType(),
                order.timedOut(LocalDateTime.now(), timeoutMinutes),
                timeoutMinutes,
                remainingPaySeconds(order, timeoutMinutes),
                order.itemCount(),
                order.completedAt(),
                firstItem != null ? firstItem.skuId() : null,
                firstItem != null ? firstItem.spuId() : null,
                firstItem != null ? firstItem.skuName() : null,
                firstItem != null ? firstItem.skuImageUrl() : null
        );
    }

    public OrderDetailView toDetail(Order order, long timeoutMinutes) {
        return new OrderDetailView(
                order.id(),
                order.orderNo(),
                order.userId(),
                order.orderStatus(),
                order.totalAmountCent(),
                order.payAmountCent(),
                order.freightAmountCent(),
                order.discountAmountCent(),
                order.payType(),
                order.remark(),
                order.receiverName(),
                order.receiverPhone(),
                order.receiverProvinceName(),
                order.receiverCityName(),
                order.receiverDistrictName(),
                order.receiverDetailAddress(),
                order.paidAt(),
                order.cancelledAt(),
                order.shippedAt(),
                order.completedAt(),
                order.timedOut(LocalDateTime.now(), timeoutMinutes),
                timeoutMinutes,
                remainingPaySeconds(order, timeoutMinutes),
                order.items().stream().map(item -> new OrderItemView(
                        item.id(),
                        item.skuId(),
                        item.spuId(),
                        item.skuName(),
                        item.skuImageUrl(),
                        item.salePriceCent(),
                        item.quantity(),
                        item.totalAmountCent()
                )).toList()
        );
    }

    public OrderPaidPreviewView toPaidPreview(Order order) {
        return new OrderPaidPreviewView(
                order.orderNo(),
                order.userId(),
                order.payAmountCent(),
                order.orderStatus()
        );
    }

    private long remainingPaySeconds(Order order, long timeoutMinutes) {
        if (!order.pendingPayment()) {
            return 0L;
        }
        LocalDateTime deadline = order.createdAtFromOrderNo().plusMinutes(timeoutMinutes);
        long seconds = Duration.between(LocalDateTime.now(), deadline).getSeconds();
        return Math.max(0L, seconds);
    }
}
