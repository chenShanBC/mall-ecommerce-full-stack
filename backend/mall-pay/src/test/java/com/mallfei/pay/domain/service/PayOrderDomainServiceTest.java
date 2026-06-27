package com.mallfei.pay.domain.service;

import com.mallfei.order.domain.model.Order;
import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.pay.domain.repository.PayOrderRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("mall-pay 支付领域服务纯单元测试")
class PayOrderDomainServiceTest extends BaseUnitTest {

    @Mock
    private PayOrderRepository payOrderRepository;

    @InjectMocks
    private PayOrderDomainService payOrderDomainService;

    @Test
    @DisplayName("正向业务流程：待支付订单可创建 MOCK 支付单并生成幂等键")
    void createPendingShouldBuildPayOrderWithIdempotentKey() {
        // Given：订单处于待支付状态，调用方未指定支付渠道。
        Order order = pendingOrder();

        // When
        PayOrder payOrder = payOrderDomainService.createPending("PAY100", order, null);

        // Then：默认 MOCK 渠道并生成业务幂等键；纯构造逻辑不访问仓储。
        assertThat(payOrder.payStatus()).isEqualTo(PayOrder.STATUS_PENDING);
        assertThat(payOrder.payChannel()).isEqualTo(PayOrder.CHANNEL_MOCK);
        assertThat(payOrder.idempotentKey()).isEqualTo("PAY:MOCK:" + order.orderNo());
        verifyNoInteractions(payOrderRepository);
    }

    @Test
    @DisplayName("支付回调幂等：成功支付单再次收到成功回调时保持成功状态")
    void successfulCallbackShouldBeIdempotent() {
        // Given：支付单已处理过成功回调。
        PayOrder success = PayOrder.createPending("PAY100", "ORD100", 1L, 1000L, "MOCK")
                .markSuccess("TRADE100", LocalDateTime.now());

        // When：重复回调再次到达。
        PayOrder second = success.markSuccess("TRADE100", LocalDateTime.now().plusSeconds(1));

        // Then：实体保持成功状态，不重复生成状态变化。
        assertThat(second).isSameAs(success);
        assertThat(second.payStatus()).isEqualTo(PayOrder.STATUS_SUCCESS);
    }

    @Test
    @DisplayName("重复支付：非待支付订单不能复用支付单并返回 COMMON_400")
    void ensureExistingPayOrderUsableShouldRejectInvalidDuplicatePayment() {
        // Given：订单已支付，但调用方试图继续复用待支付支付单。
        Order paidOrder = pendingOrder().markPaid(LocalDateTime.now());
        PayOrder pendingPayOrder = PayOrder.createPending("PAY100", paidOrder.orderNo(), 1L, 1000L, "MOCK");

        // When
        Throwable throwable = catchThrowable(() -> payOrderDomainService.ensureExistingPayOrderUsable(pendingPayOrder, paidOrder));

        // Then：重复支付在领域层被拦截。
        assertBadRequest(throwable, "当前订单状态不允许继续创建支付单");
    }

    @Test
    @DisplayName("支付超时：未成功支付单可关闭，成功支付单禁止超时关闭并返回 COMMON_400")
    void closeShouldSupportPaymentTimeoutButRejectSuccessOrder() {
        // Given：存在一个待支付支付单。
        PayOrder pending = PayOrder.createPending("PAY100", "ORD100", 1L, 1000L, "MOCK");

        // When：支付超时关闭。
        PayOrder closed = pending.close("PAY_TIMEOUT");
        Throwable throwable = catchThrowable(() -> pending.markSuccess("TRADE100", LocalDateTime.now()).close("PAY_TIMEOUT"));

        // Then：待支付可关闭，已成功不可关闭。
        assertThat(closed.payStatus()).isEqualTo(PayOrder.STATUS_CLOSED);
        assertBadRequest(throwable, "已支付成功或已退款的支付单不能关闭");
    }

    @Test
    @DisplayName("异常场景：支付金额与订单金额不一致时对账失败")
    void reconcileAmountShouldDetectAmountMismatch() {
        // Given：支付单金额小于订单应付金额。
        Order order = pendingOrder();
        PayOrder payOrder = PayOrder.createPending("PAY100", order.orderNo(), 1L, 999L, "MOCK");

        // When & Then：金额对账失败，交由应用服务标记支付异常。
        assertThat(payOrderDomainService.reconcileAmount(order, payOrder)).isFalse();
    }

    private Order pendingOrder() {
        return Order.createPending("ORD100", 1L, 1000L, "张三", "13800138000", "北京市", "北京市", "朝阳区", "望京", "备注", LocalDateTime.now().plusMinutes(15), List.of());
    }
}
