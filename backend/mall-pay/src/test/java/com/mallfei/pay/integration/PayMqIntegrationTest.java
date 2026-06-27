package com.mallfei.pay.integration;

import com.mallfei.order.application.dto.OrderRefundFailedEvent;
import com.mallfei.order.application.dto.OrderRefundSucceededEvent;
import com.mallfei.order.config.OrderMqConfig;
import com.mallfei.pay.application.service.PayRefundResultPublisher;
import com.mallfei.testsupport.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = PayRefundResultPublisher.class)
@DisplayName("mall-pay 集成测试：退款结果消息发布链路")
class PayMqIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PayRefundResultPublisher payRefundResultPublisher;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("退款成功时应向订单事件交换机发布退款成功事件")
    void publishSucceededShouldSendRefundSucceededEvent() {
        // Given：支付渠道退款成功，RabbitTemplate 使用 Mock，避免连接真实 RabbitMQ。
        String orderNo = "ORDER-INTEGRATION-1001";
        String refundNo = "REFUND-INTEGRATION-1001";

        // When
        payRefundResultPublisher.publishSucceeded(orderNo, refundNo, 1000L, "CHANNEL-REFUND-1001");

        // Then：验证支付模块到订单模块的事件契约。
        verify(rabbitTemplate).convertAndSend(
                eq(OrderMqConfig.ORDER_EVENT_EXCHANGE),
                eq(OrderMqConfig.ORDER_REFUND_SUCCEEDED_ROUTING_KEY),
                org.mockito.ArgumentMatchers.<Object>argThat(event -> event instanceof OrderRefundSucceededEvent refundEvent
                        && orderNo.equals(refundEvent.orderNo())
                        && refundNo.equals(refundEvent.refundNo())
                        && Long.valueOf(1000L).equals(refundEvent.refundAmountCent())
                        && "CHANNEL-REFUND-1001".equals(refundEvent.channelRefundNo()))
        );
    }

    @Test
    @DisplayName("退款失败时应向订单事件交换机发布退款失败事件")
    void publishFailedShouldSendRefundFailedEvent() {
        // Given：支付渠道退款失败，失败原因需要通过 MQ 通知订单模块。
        String orderNo = "ORDER-INTEGRATION-1002";
        String refundNo = "REFUND-INTEGRATION-1002";

        // When
        payRefundResultPublisher.publishFailed(orderNo, refundNo, 1000L, "渠道退款失败");

        // Then：验证失败事件的交换机、路由键和载荷契约。
        verify(rabbitTemplate).convertAndSend(
                eq(OrderMqConfig.ORDER_EVENT_EXCHANGE),
                eq(OrderMqConfig.ORDER_REFUND_FAILED_ROUTING_KEY),
                org.mockito.ArgumentMatchers.<Object>argThat(event -> event instanceof OrderRefundFailedEvent refundEvent
                        && orderNo.equals(refundEvent.orderNo())
                        && refundNo.equals(refundEvent.refundNo())
                        && Long.valueOf(1000L).equals(refundEvent.refundAmountCent())
                        && "渠道退款失败".equals(refundEvent.failReason()))
        );
    }
}
