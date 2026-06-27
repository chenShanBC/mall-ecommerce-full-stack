package com.mallfei.cart.domain.service;

import com.mallfei.cart.domain.model.CartItem;
import com.mallfei.cart.domain.repository.CartItemRepository;
import com.mallfei.testsupport.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("mall-cart 购物车领域服务纯单元测试")
class CartDomainServiceTest extends BaseUnitTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartDomainService cartDomainService;

    @Test
    @DisplayName("正向业务流程：同一用户重复添加同一 SKU 时合并数量并保留勾选状态")
    void addItemShouldMergeQuantityWhenSkuAlreadyExists() {
        // Given：购物车中已经存在同一用户、同一 SKU 的商品行。
        CartItem existing = item(1L, 10L, 100L, 2, true);
        when(cartItemRepository.findByUserIdAndSkuId(10L, 100L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.update(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When：用户再次添加相同 SKU。
        CartItem merged = cartDomainService.addItem(10L, 100L, 3, null);

        // Then：走更新分支，数量累加，不新增重复行。
        assertThat(merged.quantity()).isEqualTo(5);
        assertThat(merged.checked()).isTrue();
        verify(cartItemRepository).update(argThat(cartItem -> cartItem.id().equals(1L) && cartItem.quantity() == 5));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("边界值：未勾选任何购物车项时禁止结算并返回 COMMON_400")
    void ensureCanCheckoutShouldRejectEmptyItems() {
        // Given：结算列表为空。
        List<CartItem> selectedItems = List.of();

        // When
        Throwable throwable = catchThrowable(() -> cartDomainService.ensureCanCheckout(selectedItems));

        // Then：参数类业务异常必须断言错误码，避免接口层误判。
        assertBadRequest(throwable, "未勾选可结算购物车项");
    }

    @Test
    @DisplayName("异常场景：不能操作其他用户的购物车项并返回 AUTH_403")
    void loadOwnedItemShouldRejectOtherUsersItem() {
        // Given：目标购物车项属于其他用户。
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item(1L, 99L, 100L, 1, true)));

        // When
        Throwable throwable = catchThrowable(() -> cartDomainService.loadOwnedItem(10L, 1L));

        // Then：越权类异常统一归口 AUTH_403。
        assertForbidden(throwable, "无权操作当前购物车项");
    }

    @Test
    @DisplayName("正向业务流程：批量加载购物车项时去重，避免重复结算")
    void loadOwnedItemsShouldDeduplicateCartItemIds() {
        // Given：入参包含重复购物车 ID。
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item(1L, 10L, 100L, 1, true)));
        when(cartItemRepository.findById(2L)).thenReturn(Optional.of(item(2L, 10L, 101L, 2, true)));

        // When
        List<CartItem> items = cartDomainService.loadOwnedItems(10L, List.of(1L, 1L, 2L));

        // Then：仓储只按去重后的 ID 查询，避免重复结算。
        assertThat(items).hasSize(2);
        verify(cartItemRepository, times(1)).findById(1L);
        verify(cartItemRepository, times(1)).findById(2L);
    }

    @Test
    @DisplayName("并发风险场景：多线程重复添加同一 SKU 时必须走仓储更新分支")
    void concurrentAddSameSkuShouldUseUpdatePath() throws Exception {
        // Given：多线程同时命中同一已有购物车行。
        CartItem existing = item(1L, 10L, 100L, 1, true);
        AtomicInteger updateCount = new AtomicInteger();
        when(cartItemRepository.findByUserIdAndSkuId(10L, 100L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.update(any(CartItem.class))).thenAnswer(invocation -> {
            updateCount.incrementAndGet();
            return invocation.getArgument(0);
        });
        int threads = 8;
        var executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        // When
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                cartDomainService.addItem(10L, 100L, 1, true);
                latch.countDown();
            });
        }
        latch.await();
        executor.shutdownNow();

        // Then：领域层暴露数据库乐观锁/行锁保护点，不能走新增分支。
        assertThat(updateCount).hasValue(threads);
        verify(cartItemRepository, never()).save(any());
    }

    private CartItem item(Long id, Long userId, Long skuId, int quantity, boolean checked) {
        return new CartItem(id, userId, skuId, quantity, checked, LocalDateTime.now(), LocalDateTime.now());
    }
}
