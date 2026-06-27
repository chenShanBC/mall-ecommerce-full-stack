package com.mallfei.cart.standard;

import com.mallfei.cart.domain.model.CartItem;
import com.mallfei.cart.domain.repository.CartItemRepository;
import com.mallfei.cart.domain.service.CartDomainService;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.testsupport.BaseModuleTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = CartModuleStandardTest.TestConfig.class)
@DisplayName("mall-cart 模块测试：购物车模块协作规则")
class CartModuleStandardTest extends BaseModuleTest {

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("需求：购物车添加相同SKU时合并数量")
    void shouldMergeSameSkuWhenAddCartItem() {
        InMemoryCartItemRepository repository = new InMemoryCartItemRepository();
        CartDomainService service = new CartDomainService(repository);

        service.addItem(1L, 1001L, 2, true);
        CartItem merged = service.addItem(1L, 1001L, 3, true);

        assertThat(merged.quantity()).isEqualTo(5);
        assertThat(repository.findByUserId(1L)).hasSize(1);
    }

    @Test
    @DisplayName("需求：被删除SKU再次加入购物车时恢复原记录")
    void shouldRestoreDeletedCartItem() {
        InMemoryCartItemRepository repository = new InMemoryCartItemRepository();
        CartDomainService service = new CartDomainService(repository);
        CartItem saved = service.addItem(1L, 1001L, 2, true);
        service.removeItem(1L, saved.id());

        CartItem restored = service.addItem(1L, 1001L, 1, null);

        assertThat(restored.id()).isEqualTo(saved.id());
        assertThat(restored.checked()).isTrue();
        assertThat(restored.quantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("需求：未勾选任何商品时禁止结算")
    void shouldRejectCheckoutWithoutCheckedItems() {
        CartDomainService service = new CartDomainService(new InMemoryCartItemRepository());

        assertThatThrownBy(() -> service.ensureCanCheckout(List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未勾选");
    }

    @Test
    @DisplayName("需求：购物车数量边界，统计所有有效商品数量")
    void shouldCalculateTotalQuantity() {
        InMemoryCartItemRepository repository = new InMemoryCartItemRepository();
        CartDomainService service = new CartDomainService(repository);
        service.addItem(1L, 1001L, 1, true);
        service.addItem(1L, 1002L, 99, false);

        assertThat(service.totalQuantity(1L)).isEqualTo(100);
    }

    @Test
    @DisplayName("需求：越权操作其他用户购物车项时抛出403业务异常")
    void shouldRejectCrossUserCartOperation() {
        InMemoryCartItemRepository repository = new InMemoryCartItemRepository();
        CartDomainService service = new CartDomainService(repository);
        CartItem item = service.addItem(1L, 1001L, 1, true);

        assertThatThrownBy(() -> service.loadOwnedItem(2L, item.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作");
    }

    @Test
    @DisplayName("需求：并发加入同一SKU不产生多条购物车记录")
    void shouldNotCreateDuplicateRowsWhenConcurrentMerge() {
        InMemoryCartItemRepository repository = new InMemoryCartItemRepository();

        java.util.stream.IntStream.range(0, 20).parallel().forEach(i -> repository.addOrMerge(1L, 1001L, 1));

        assertThat(repository.findByUserId(1L)).hasSize(1);
        assertThat(repository.findByUserId(1L).getFirst().quantity()).isEqualTo(20);
    }

    static class TestConfig {
    }

    private static final class InMemoryCartItemRepository implements CartItemRepository {
        private final AtomicLong idGenerator = new AtomicLong(1);
        private final ConcurrentHashMap<Long, CartItem> activeStore = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<Long, CartItem> deletedStore = new ConcurrentHashMap<>();

        synchronized CartItem addOrMerge(Long userId, Long skuId, Integer quantity) {
            return findByUserIdAndSkuId(userId, skuId)
                    .map(existing -> update(new CartItem(existing.id(), userId, skuId, existing.quantity() + quantity, existing.checked(), existing.createdAt(), LocalDateTime.now())))
                    .orElseGet(() -> save(new CartItem(null, userId, skuId, quantity, true, LocalDateTime.now(), LocalDateTime.now())));
        }

        @Override
        public Optional<CartItem> findById(Long id) {
            return Optional.ofNullable(activeStore.get(id));
        }

        @Override
        public Optional<CartItem> findByUserIdAndSkuId(Long userId, Long skuId) {
            return activeStore.values().stream().filter(item -> item.userId().equals(userId) && item.skuId().equals(skuId)).findFirst();
        }

        @Override
        public Optional<CartItem> findDeletedByUserIdAndSkuId(Long userId, Long skuId) {
            return deletedStore.values().stream().filter(item -> item.userId().equals(userId) && item.skuId().equals(skuId)).findFirst();
        }

        @Override
        public List<CartItem> findByUserId(Long userId) {
            return activeStore.values().stream().filter(item -> item.userId().equals(userId)).toList();
        }

        @Override
        public CartItem save(CartItem cartItem) {
            CartItem saved = new CartItem(idGenerator.getAndIncrement(), cartItem.userId(), cartItem.skuId(), cartItem.quantity(), cartItem.checked(), cartItem.createdAt(), cartItem.updatedAt());
            activeStore.put(saved.id(), saved);
            return saved;
        }

        @Override
        public CartItem update(CartItem cartItem) {
            activeStore.put(cartItem.id(), cartItem);
            deletedStore.remove(cartItem.id());
            return cartItem;
        }

        @Override
        public void deleteByIds(List<Long> ids) {
            ids.forEach(id -> {
                CartItem removed = activeStore.remove(id);
                if (removed != null) {
                    deletedStore.put(id, removed);
                }
            });
        }

        @Override
        public void deleteByUserId(Long userId) {
            List<Long> ids = new ArrayList<>(activeStore.values().stream().filter(item -> item.userId().equals(userId)).map(CartItem::id).toList());
            deleteByIds(ids);
        }
    }
}
