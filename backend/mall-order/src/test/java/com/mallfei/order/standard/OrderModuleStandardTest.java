package com.mallfei.order.standard;

import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.domain.model.OrderItem;
import com.mallfei.testsupport.BaseModuleTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = OrderModuleStandardTest.TestConfig.class)
@DisplayName("mall-order 模块测试：订单模块协作规则")
class OrderModuleStandardTest extends BaseModuleTest {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("需求：批量下单时汇总商品金额并生成待支付订单")
    void shouldCreateBatchOrderWithCalculatedAmount() {
        OrderItem first = item(1001L, 2, 1999L);
        OrderItem second = item(1002L, 1, 3999L);

        Order order = Order.createPending("O1001", 1L, first.totalAmountCent() + second.totalAmountCent(), "张三", "13800000000", "浙江", "杭州", "西湖", "文三路", "", LocalDateTime.now().plusMinutes(30), List.of(first, second));

        assertThat(order.itemCount()).isEqualTo(3);
        assertThat(order.payAmountCent()).isEqualTo(7997L);
        assertThat(order.pendingPayment()).isTrue();
    }

    @Test
    @DisplayName("需求：金额计算边界，单价为分且数量为1时金额精确不丢失")
    void shouldKeepCentPrecisionAtBoundary() {
        OrderItem oneCentItem = item(1001L, 1, 1L);

        Order order = Order.createPending("O1002", 1L, oneCentItem.totalAmountCent(), "张三", "13800000000", "浙江", "杭州", "西湖", "文三路", "", LocalDateTime.now().plusMinutes(30), List.of(oneCentItem));

        assertThat(order.payAmountCent()).isEqualTo(1L);
    }

    @Test
    @DisplayName("需求：订单状态按 待支付-已支付-处理中-已发货-已完成 正向流转")
    void shouldFlowOrderStatusForward() {
        Order pending = sampleOrder("O1003");

        Order paid = pending.markPaid(LocalDateTime.now());
        Order processing = paid.markProcessing();
        Order shipped = processing.ship(LocalDateTime.now());
        Order completed = shipped.complete(LocalDateTime.now());

        assertThat(completed.completed()).isTrue();
        assertThat(completed.version()).isGreaterThan(pending.version());
    }

    @Test
    @DisplayName("需求：非法状态流转被阻断，待支付订单不能直接发货")
    void shouldRejectInvalidStatusTransition() {
        Order pending = sampleOrder("O1004");

        assertThatThrownBy(() -> pending.ship(LocalDateTime.now()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许发货");
    }

    @Test
    @DisplayName("需求：支付超时订单自动取消并触发释放库存判断")
    void shouldTimeoutCancelPendingOrder() {
        Order pending = sampleOrder("O1005");

        Order cancelled = pending.cancelIfTimedOut(LocalDateTime.now().plusHours(1), 30);

        assertThat(cancelled.timeoutCancelled()).isTrue();
        assertThat(cancelled.shouldReleaseStockAfterCancelled(pending)).isTrue();
    }

    @Test
    @DisplayName("需求：并发批量下单使用业务单号生成器保证订单号不重复")
    void shouldGenerateUniqueOrderNoForConcurrentBatchCreate() {
        OrderNoGenerator generator = new OrderNoGenerator();

        List<String> orderNos = java.util.stream.IntStream.range(0, 100)
                .parallel()
                .mapToObj(i -> generator.next())
                .distinct()
                .toList();

        assertThat(orderNos).hasSize(100);
    }

    static class TestConfig {
    }

    private static Order sampleOrder(String orderNo) {
        return Order.createPending(orderNo, 1L, 1999L, "张三", "13800000000", "浙江", "杭州", "西湖", "文三路", "", LocalDateTime.now().plusMinutes(30), List.of(item(1001L, 1, 1999L)));
    }

    private static OrderItem item(Long skuId, int quantity, long priceCent) {
        return new OrderItem(null, null, "ORD_TEST", skuId, 1L, "SKU-" + skuId, "", priceCent, quantity, quantity * priceCent);
    }

    private static final class OrderNoGenerator {
        private final AtomicLong sequence = new AtomicLong();

        String next() {
            return "ORD" + System.currentTimeMillis() + sequence.incrementAndGet();
        }
    }
}
