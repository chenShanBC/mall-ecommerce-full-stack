package com.mallfei.order.integration;

import com.mallfei.order.application.dto.OrderTimeoutEvent;
import com.mallfei.order.application.service.OrderTimeoutEventPublisher;
import com.mallfei.order.config.OrderMqConfig;
import com.mallfei.testsupport.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = OrderTimeoutEventPublisher.class)
@DisplayName("mall-order 集成测试：订单超时消息发布链路")
class OrderMqIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrderTimeoutEventPublisher orderTimeoutEventPublisher;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("创建订单后发布延迟超时事件时应写入指定交换机和路由键")
    void publishTimeoutEventShouldSendDelayMessage() {
        // Given：订单创建成功后需要发布一条延迟超时消息，RabbitTemplate 使用 Mock，避免连接真实 RabbitMQ。
        OrderTimeoutEvent event = new OrderTimeoutEvent("ORDER-INTEGRATION-1001");
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(2);

        // When
        orderTimeoutEventPublisher.publish(event, expireTime);

        // Then：集成测试验证 Spring Bean 装配 + MQ 发布边界，不触碰真实 MQ 和业务数据。
        verify(rabbitTemplate).convertAndSend(
                eq(OrderMqConfig.ORDER_TIMEOUT_DELAY_EXCHANGE),
                eq(OrderMqConfig.ORDER_TIMEOUT_DELAY_ROUTING_KEY),
                eq(event),
                any(MessagePostProcessor.class)
        );
    }
}
