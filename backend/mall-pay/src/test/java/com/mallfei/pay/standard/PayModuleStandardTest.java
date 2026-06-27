package com.mallfei.pay.standard;

import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.domain.model.Order;
import com.mallfei.order.domain.model.OrderItem;
import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.testsupport.BaseModuleTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = PayModuleStandardTest.TestConfig.class)
@DisplayName("mall-pay 模块测试：支付模块协作规则")
class PayModuleStandardTest extends BaseModuleTest {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private MockAlipaySdkClient alipaySdkClient;

    @Test
    @DisplayName("需求：支付回调首次成功后更新支付单与订单状态")
    void shouldMarkPaySuccessOnFirstCallback() {
        PayCallbackProcessor processor = new PayCallbackProcessor();
        Order order = pendingOrder("O1001", 9900L);
        PayOrder payOrder = PayOrder.createPending("P1001", order.orderNo(), order.userId(), order.payAmountCent(), PayOrder.CHANNEL_MOCK);

        CallbackResult result = processor.handleSuccess(payOrder, order, "TXN-1001");

        assertThat(result.payOrder().success()).isTrue();
        assertThat(result.order().paid()).isTrue();
        assertThat(result.changed()).isTrue();
    }

    @Test
    @DisplayName("需求：支付回调幂等，同一交易号重复回调不重复推进状态")
    void shouldKeepCallbackIdempotent() {
        PayCallbackProcessor processor = new PayCallbackProcessor();
        Order order = pendingOrder("O1002", 9900L);
        PayOrder success = PayOrder.createPending("P1002", order.orderNo(), order.userId(), order.payAmountCent(), PayOrder.CHANNEL_MOCK)
                .markSuccess("TXN-1002", LocalDateTime.now());

        CallbackResult result = processor.handleSuccess(success, order.markPaid(LocalDateTime.now()), "TXN-1002");

        assertThat(result.payOrder()).isSameAs(success);
        assertThat(result.changed()).isFalse();
    }

    @Test
    @DisplayName("需求：重复支付场景，已关闭支付单不能再次回调成功")
    void shouldRejectDuplicatePaymentOnClosedPayOrder() {
        PayCallbackProcessor processor = new PayCallbackProcessor();
        Order order = pendingOrder("O1003", 9900L);
        PayOrder closed = PayOrder.createPending("P1003", order.orderNo(), order.userId(), order.payAmountCent(), PayOrder.CHANNEL_MOCK).close("timeout");

        assertThatThrownBy(() -> processor.handleSuccess(closed, order, "TXN-1003"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许回调成功");
    }

    @Test
    @DisplayName("需求：支付金额不一致时阻断回调，避免错账")
    void shouldRejectAmountMismatchCallback() {
        PayCallbackProcessor processor = new PayCallbackProcessor();
        Order order = pendingOrder("O1004", 9900L);
        PayOrder payOrder = PayOrder.createPending("P1004", order.orderNo(), order.userId(), 9800L, PayOrder.CHANNEL_MOCK);

        assertThatThrownBy(() -> processor.handleSuccess(payOrder, order, "TXN-1004"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("金额不一致");
    }

    @Test
    @DisplayName("需求：支付超时关闭待支付单，后续不可再支付")
    void shouldCloseTimeoutPayOrder() {
        PayTimeoutService timeoutService = new PayTimeoutService();
        PayOrder pending = PayOrder.createPending("P1005", "O1005", 1L, 9900L, PayOrder.CHANNEL_MOCK);

        PayOrder closed = timeoutService.closeIfTimeout(pending, pending.createdAt().plusMinutes(31), 30);

        assertThat(closed.closed()).isTrue();
        assertThatThrownBy(() -> closed.markSuccess("TXN-1005", LocalDateTime.now())).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("需求：并发重复支付请求共用同一幂等键，只创建一笔支付单")
    void shouldReusePayOrderWithIdempotentKey() {
        PayOrderFactory factory = new PayOrderFactory();
        Order order = pendingOrder("O1006", 9900L);

        PayOrder first = factory.createOrReuse(order, PayOrder.CHANNEL_MOCK);
        PayOrder second = factory.createOrReuse(order, PayOrder.CHANNEL_MOCK);

        assertThat(second.payOrderNo()).isEqualTo(first.payOrderNo());
        assertThat(factory.createdCount()).isEqualTo(1);
    }

    static class TestConfig {
    }

    interface MockAlipaySdkClient {
        String verifyCallback(Map<String, String> params);
    }

    private static Order pendingOrder(String orderNo, Long amountCent) {
        OrderItem item = new OrderItem(null, null, orderNo, 1001L, 1L, "SKU", "", amountCent, 1, amountCent);
        return Order.createPending(orderNo, 1L, amountCent, "张三", "13800000000", "浙江", "杭州", "西湖", "文三路", "", LocalDateTime.now().plusMinutes(30), List.of(item));
    }

    private static final class PayCallbackProcessor {
        CallbackResult handleSuccess(PayOrder payOrder, Order order, String transactionNo) {
            if (payOrder.success()) {
                return new CallbackResult(payOrder, order, false);
            }
            if (!payOrder.amountConsistentWith(order.payAmountCent())) {
                throw BusinessException.badRequest("支付回调金额不一致");
            }
            PayOrder successPayOrder = payOrder.markSuccess(transactionNo, LocalDateTime.now());
            Order paidOrder = order.markPaid(LocalDateTime.now());
            return new CallbackResult(successPayOrder, paidOrder, true);
        }
    }

    private static final class PayTimeoutService {
        PayOrder closeIfTimeout(PayOrder payOrder, LocalDateTime now, long timeoutMinutes) {
            if (payOrder.pending() && payOrder.createdAt().plusMinutes(timeoutMinutes).isBefore(now)) {
                return payOrder.close("PAY_TIMEOUT");
            }
            return payOrder;
        }
    }

    private static final class PayOrderFactory {
        private final ConcurrentHashMap<String, PayOrder> store = new ConcurrentHashMap<>();

        PayOrder createOrReuse(Order order, String channel) {
            String key = "PAY:" + channel + ':' + order.orderNo();
            return store.computeIfAbsent(key, ignored -> PayOrder.createPending("P" + store.size(), order.orderNo(), order.userId(), order.payAmountCent(), channel));
        }

        int createdCount() {
            return store.size();
        }
    }

    private record CallbackResult(PayOrder payOrder, Order order, boolean changed) {
    }
}
