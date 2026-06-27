package com.mallfei.testsupport;

import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * MQ 副作用验证工具，统一表达消息发布期望。
 */
public final class MqVerifySupport {

    private MqVerifySupport() {
    }

    public static void verifyNoMqPublished(RabbitTemplate rabbitTemplate) {
        Mockito.verifyNoInteractions(rabbitTemplate);
    }
}
