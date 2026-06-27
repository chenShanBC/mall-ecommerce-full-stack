package com.mallfei.cart.integration;

import com.mallfei.cart.application.service.CartApplicationService;
import com.mallfei.cart.application.vo.CartQuantityView;
import com.mallfei.cart.controller.CartController;
import com.mallfei.testsupport.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@DisplayName("mall-cart 集成测试：购物车 HTTP API 链路")
class CartApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartApplicationService cartApplicationService;

    @Test
    @DisplayName("查询购物车数量接口应返回统一响应结构")
    void quantityApiShouldReturnUnifiedResponse() throws Exception {
        // Given：应用服务返回购物车数量统计，服务层使用 Mock，避免连接真实数据库和 Redis。
        when(cartApplicationService.currentQuantity()).thenReturn(new CartQuantityView(2, 5));

        // When & Then：验证 Controller、JSON 序列化和统一 ApiResponse 的 HTTP 集成链路。
        mockMvc.perform(get("/api/cart/quantity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.itemCount").value(2))
                .andExpect(jsonPath("$.data.totalQuantity").value(5));
        verify(cartApplicationService).currentQuantity();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        CartController cartController(CartApplicationService cartApplicationService) {
            return new CartController(cartApplicationService);
        }
    }
}
