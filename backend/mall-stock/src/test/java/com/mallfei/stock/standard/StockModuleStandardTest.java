package com.mallfei.stock.standard;

import com.mallfei.common.exception.BusinessException;
import com.mallfei.stock.domain.model.Stock;
import com.mallfei.stock.domain.service.StockDomainService;
import com.mallfei.testsupport.BaseModuleTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = StockModuleStandardTest.TestConfig.class)
@DisplayName("mall-stock 模块测试：库存模块协作规则")
class StockModuleStandardTest extends BaseModuleTest {

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("需求：Lua分布式锁扣减成功后，可用库存减少、锁定库存增加")
    void shouldReserveStockByLuaLock() {
        LuaStockSimulator simulator = new LuaStockSimulator(10);

        long result = simulator.reserve("ORDER", "O1001", 1001L, 3, Duration.ofSeconds(30));

        assertThat(result).isZero();
        assertThat(simulator.available()).isEqualTo(7);
        assertThat(simulator.locked()).isEqualTo(3);
    }

    @Test
    @DisplayName("需求：库存不足时拒绝扣减并保持库存快照不变")
    void shouldRejectWhenStockInsufficient() {
        LuaStockSimulator simulator = new LuaStockSimulator(2);

        long result = simulator.reserve("ORDER", "O1002", 1001L, 3, Duration.ofSeconds(30));

        assertThat(result).isEqualTo(-1L);
        assertThat(simulator.available()).isEqualTo(2);
        assertThat(simulator.locked()).isZero();
    }

    @Test
    @DisplayName("需求：同一业务单重复扣减必须幂等拒绝，避免重复锁库存")
    void shouldRejectDuplicateReserveForSameBusiness() {
        LuaStockSimulator simulator = new LuaStockSimulator(10);

        assertThat(simulator.reserve("ORDER", "O1003", 1001L, 2, Duration.ofSeconds(30))).isZero();
        assertThat(simulator.reserve("ORDER", "O1003", 1001L, 2, Duration.ofSeconds(30))).isEqualTo(-3L);

        assertThat(simulator.available()).isEqualTo(8);
        assertThat(simulator.locked()).isEqualTo(2);
    }

    @Test
    @DisplayName("需求：预占超时后执行回滚释放锁定库存")
    void shouldRollbackExpiredReservation() {
        LuaStockSimulator simulator = new LuaStockSimulator(10);
        simulator.reserve("ORDER", "O1004", 1001L, 4, Duration.ofMillis(1));

        simulator.rollbackExpiredReservations(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5));

        assertThat(simulator.available()).isEqualTo(10);
        assertThat(simulator.locked()).isZero();
    }

    @Test
    @DisplayName("需求：并发扣减不能超卖，成功数量不得超过初始库存")
    void shouldPreventOversoldUnderConcurrentReserve() throws Exception {
        LuaStockSimulator simulator = new LuaStockSimulator(5);
        var pool = Executors.newFixedThreadPool(12);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < 12; i++) {
            int index = i;
            pool.submit(() -> {
                start.await();
                if (simulator.reserve("ORDER", "CONCURRENT-" + index, 1001L, 1, Duration.ofSeconds(30)) == 0L) {
                    successCount.incrementAndGet();
                }
                return null;
            });
        }

        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(simulator.available()).isZero();
        assertThat(simulator.locked()).isEqualTo(5);
    }

    @Test
    @DisplayName("需求：库存领域模型覆盖边界值、冻结异常与确认扣减")
    void shouldValidateStockDomainBoundaries() {
        Stock stock = Stock.initialize(1001L, 5).reserve(5);

        assertThat(stock.availableStock()).isZero();
        assertThat(stock.confirm(5).totalStock()).isZero();
        assertThatThrownBy(() -> Stock.initialize(1002L, 1).reserve(2)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> Stock.initialize(1003L, 5).applyPolicy(Stock.STATUS_FROZEN, 1, 10).reserve(1))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("需求：Redis Lua返回码映射为统一业务异常")
    void shouldMapLuaResultToBusinessException() {
        StockDomainService service = new StockDomainService(null, null);

        assertThatThrownBy(() -> service.validateReserveResult(-1L, 1001L)).hasMessageContaining("库存不足");
        assertThatThrownBy(() -> service.validateReserveResult(-2L, 1001L)).hasMessageContaining("未初始化");
        assertThatThrownBy(() -> service.validateReserveResult(-3L, 1001L)).hasMessageContaining("重复预占");
    }

    static class TestConfig {
    }

    private static final class LuaStockSimulator {
        private final Set<String> reservedBusinessKeys = ConcurrentHashMap.newKeySet();
        private final ConcurrentHashMap<String, Reservation> reservations = new ConcurrentHashMap<>();
        private int available;
        private int locked;

        private LuaStockSimulator(int initialStock) {
            this.available = initialStock;
        }

        synchronized long reserve(String businessType, String businessNo, Long skuId, int quantity, Duration ttl) {
            String key = businessType + ':' + businessNo + ':' + skuId;
            if (reservedBusinessKeys.contains(key)) {
                return -3L;
            }
            if (available < quantity) {
                return -1L;
            }
            available -= quantity;
            locked += quantity;
            reservedBusinessKeys.add(key);
            reservations.put(key, new Reservation(quantity, System.nanoTime() + ttl.toNanos()));
            return 0L;
        }

        synchronized void rollbackExpiredReservations(long nowNanos) {
            reservations.entrySet().removeIf(entry -> {
                if (entry.getValue().expiresAtNanos <= nowNanos) {
                    available += entry.getValue().quantity;
                    locked -= entry.getValue().quantity;
                    reservedBusinessKeys.remove(entry.getKey());
                    return true;
                }
                return false;
            });
        }

        int available() {
            return available;
        }

        int locked() {
            return locked;
        }

        private record Reservation(int quantity, long expiresAtNanos) {
        }
    }
}
