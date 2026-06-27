package com.mallfei.stock.domain.service;

import com.mallfei.stock.domain.model.Stock;
import com.mallfei.stock.domain.model.StockLockRecord;
import com.mallfei.stock.domain.repository.StockLockRepository;
import com.mallfei.stock.domain.repository.StockRepository;
import com.mallfei.testsupport.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("mall-stock 库存领域服务纯单元测试")
class StockDomainServiceTest extends BaseUnitTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockLockRepository stockLockRepository;

    @InjectMocks
    private StockDomainService stockDomainService;

    @Test
    @DisplayName("正向业务流程：Lua 分布式锁扣减返回成功码时允许预占库存")
    void validateReserveResultShouldAcceptLuaSuccessCode() {
        // Given：Redis Lua 原子扣减脚本返回成功码。
        Long luaResult = 1L;

        // When
        stockDomainService.validateReserveResult(luaResult, 100L);

        // Then：结果校验不访问数据库，预占落库由应用服务编排。
        verifyNoInteractions(stockRepository, stockLockRepository);
    }

    @Test
    @DisplayName("异常场景：Lua 扣减返回库存不足码时抛出 COMMON_400")
    void validateReserveResultShouldRejectInsufficientStock() {
        // Given：Redis Lua 原子扣减脚本返回库存不足码。
        Long luaResult = -1L;

        // When
        Throwable throwable = catchThrowable(() -> stockDomainService.validateReserveResult(luaResult, 100L));

        // Then：库存不足属于业务参数/状态异常，统一返回 COMMON_400。
        assertBadRequest(throwable, "SKU库存不足: 100");
    }

    @Test
    @DisplayName("异常场景：重复扣减同一业务单时阻断重复预占并返回 COMMON_400")
    void ensureCanCreateReservationShouldRejectDuplicateNonReservedRecord() {
        // Given：同一业务单已有非 RESERVED 状态锁定记录。
        StockLockRecord existing = new StockLockRecord(1L, "L100", 100L, "ORDER", "O100", 2, "CONFIRMED", null, null, null, true, false, true);

        // When
        Throwable throwable = catchThrowable(() -> stockDomainService.ensureCanCreateReservation(existing, 100L));

        // Then：重复扣减被领域层拦截，避免订单重试造成库存重复冻结。
        assertBadRequest(throwable, "库存锁记录状态不允许再次预占");
    }

    @Test
    @DisplayName("边界值：库存初始化时负数库存归零，避免脏数据写入")
    void initStockShouldNormalizeNegativeInitialStockToZero() {
        // Given：上游传入负库存初始化值。
        when(stockRepository.findBySkuId(100L)).thenReturn(Optional.empty());
        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Stock stock = stockDomainService.initStock(100L, -5);

        // Then：库存数被归零后保存，不允许负数进入库存账本。
        assertThat(stock.totalStock()).isZero();
        assertThat(stock.availableStock()).isZero();
        verify(stockRepository).save(argThat(saved -> saved.skuId().equals(100L) && saved.totalStock() == 0));
    }

    @Test
    @DisplayName("超时回滚场景：取消 Lua 结果为已确认时禁止回滚并返回 COMMON_400")
    void validateCancelResultShouldRejectConfirmedReservationRollback() {
        // Given：订单超时回滚时，库存锁已被确认扣减。
        Long luaResult = -3L;

        // When
        Throwable throwable = catchThrowable(() -> stockDomainService.validateCancelResult(luaResult, 100L));

        // Then：已确认库存禁止取消，避免已扣减库存被错误释放。
        assertBadRequest(throwable, "库存已确认，不能取消");
    }

    @Test
    @DisplayName("并发风险场景：并发确认库存时每次确认结果都必须校验")
    void concurrentConfirmShouldValidateEveryLuaResult() throws Exception {
        // Given：多个确认请求同时返回 Lua 成功码。
        int threads = 6;
        AtomicInteger successCount = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        // When
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                stockDomainService.validateConfirmResult(1L, 100L);
                successCount.incrementAndGet();
                latch.countDown();
            });
        }
        latch.await();
        executor.shutdownNow();

        // Then：每个并发确认结果均完成校验，原子性由 Lua 层兜底。
        assertThat(successCount).hasValue(threads);
    }
}
