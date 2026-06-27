package com.mallfei.order.domain.service;

import com.mallfei.order.application.dto.OrderCreateRequest;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.domain.model.ProductSnapshot;
import com.mallfei.order.domain.repository.OrderRepository;
import com.mallfei.order.domain.repository.ProductSnapshotRepository;
import com.mallfei.testsupport.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("mall-order 订单领域服务纯单元测试")
class OrderDomainServiceTest extends BaseUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductSnapshotRepository productSnapshotRepository;

    @InjectMocks
    private OrderDomainService orderDomainService;

    @Test
    @DisplayName("正向业务流程：批量下单合并重复 SKU 并计算订单金额")
    void createPendingOrderShouldMergeSkuAndCalculateAmount() {
        // Given：请求中同一 SKU 出现多次，商品快照价格为 1999 分。
        when(productSnapshotRepository.findBySkuIds(anyList()))
                .thenReturn(List.of(new ProductSnapshot(100L, 10L, "测试SKU", "sku.png", 1999L)));

        // When
        Order order = orderDomainService.createPendingOrder("ORD100", 1L, request(List.of(
                new OrderCreateRequest.Item(100L, 1),
                new OrderCreateRequest.Item(100L, 2)
        )), LocalDateTime.now().plusMinutes(15));

        // Then：领域层合并明细并保证金额按分精确计算。
        assertThat(order.orderStatus()).isEqualTo(Order.STATUS_PENDING_PAYMENT);
        assertThat(order.items()).hasSize(1);
        assertThat(order.items().get(0).quantity()).isEqualTo(3);
        assertThat(order.payAmountCent()).isEqualTo(5997L);
        verify(productSnapshotRepository).findBySkuIds(List.of(100L));
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("异常场景：商品快照缺失时禁止创建订单并返回 COMMON_400")
    void createPendingOrderShouldRejectMissingProductSnapshot() {
        // Given：商品快照仓储未返回目标 SKU。
        when(productSnapshotRepository.findBySkuIds(anyList())).thenReturn(List.of());

        // When
        Throwable throwable = catchThrowable(() -> orderDomainService.createPendingOrder("ORD101", 1L, request(List.of(new OrderCreateRequest.Item(999L, 1)))));

        // Then：商品快照缺失在领域层被拦截，不创建半成品订单。
        assertBadRequest(throwable, "商品SKU不存在: 999");
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("订单状态流转：待支付订单支付成功后进入已支付状态")
    void orderShouldTransitFromPendingToPaid() {
        // Given：订单处于待支付状态。
        Order pending = samplePendingOrder(LocalDateTime.now().plusMinutes(10));

        // When
        Order paid = pending.markPaid(LocalDateTime.now());

        // Then
        assertThat(paid.orderStatus()).isEqualTo(Order.STATUS_PAID);
        assertThat(paid.paidAt()).isNotNull();
    }

    @Test
    @DisplayName("边界值：支付超时订单定位后自动转为超时取消")
    void locateAllTimedOutOrdersShouldCancelExpiredPendingOrders() {
        // Given：同时存在已过期和未过期待支付订单。
        Order expired = samplePendingOrder(LocalDateTime.now().minusSeconds(1));
        Order valid = samplePendingOrder(LocalDateTime.now().plusMinutes(5));
        when(orderRepository.findAll()).thenReturn(List.of(expired, valid));

        // When
        List<Order> timedOutOrders = orderDomainService.locateAllTimedOutOrders(LocalDateTime.now(), 15);

        // Then：只定位已过期订单。
        assertThat(timedOutOrders).hasSize(1);
        assertThat(timedOutOrders.get(0).orderStatus()).isEqualTo(Order.STATUS_TIMEOUT_CANCELLED);
        verify(orderRepository).findAll();
    }

    @Test
    @DisplayName("异常场景：用户不能访问他人订单并返回 AUTH_403")
    void ensureOwnedByShouldRejectOtherUsersOrder() {
        // Given：订单归属用户为 1。
        Order order = samplePendingOrder(LocalDateTime.now().plusMinutes(5));

        // When
        Throwable throwable = catchThrowable(() -> orderDomainService.ensureOwnedBy(order, 2L));

        // Then：越权访问统一返回 AUTH_403。
        assertForbidden(throwable, "无权访问当前订单");
    }

    private OrderCreateRequest request(List<OrderCreateRequest.Item> items) {
        return new OrderCreateRequest("张三", "13800138000", "北京市", "北京市", "朝阳区", "望京", "备注", items);
    }

    private Order samplePendingOrder(LocalDateTime expireTime) {
        return Order.createPending("ORD100", 1L, 1000L, "张三", "13800138000", "北京市", "北京市", "朝阳区", "望京", "备注", expireTime, List.of());
    }
}
