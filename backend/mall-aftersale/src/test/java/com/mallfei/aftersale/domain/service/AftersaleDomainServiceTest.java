package com.mallfei.aftersale.domain.service;

import com.mallfei.aftersale.domain.model.AftersaleOrder;
import com.mallfei.aftersale.domain.repository.AftersaleOrderRepository;
import com.mallfei.testsupport.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("mall-aftersale 售后领域服务纯单元测试")
class AftersaleDomainServiceTest extends BaseUnitTest {

    @Mock
    private AftersaleOrderRepository aftersaleOrderRepository;

    @InjectMocks
    private AftersaleDomainService aftersaleDomainService;

    @Test
    @DisplayName("正向业务流程：创建仅退款售后单后保存为待审核状态")
    void saveShouldPersistPendingReviewAftersaleOrder() {
        // Given：售后单已按领域工厂构造成待审核状态。
        AftersaleOrder order = sampleOrder();
        when(aftersaleOrderRepository.save(order)).thenReturn(order);

        // When
        AftersaleOrder saved = aftersaleDomainService.save(order);

        // Then：保存动作必须明确验证，防止售后申请只构造不落库。
        assertThat(saved.status()).isEqualTo(AftersaleOrder.STATUS_PENDING_REVIEW);
        verify(aftersaleOrderRepository).save(order);
    }

    @Test
    @DisplayName("订单状态流转：待审核售后单审核通过后进入已通过状态")
    void approveShouldTransitPendingReviewToApproved() {
        // Given：售后单处于待审核状态。
        AftersaleOrder pending = sampleOrder();

        // When
        AftersaleOrder approved = pending.approve(LocalDateTime.now());

        // Then
        assertThat(approved.status()).isEqualTo(AftersaleOrder.STATUS_APPROVED);
        assertThat(approved.reviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("异常场景：非退款处理中售后单不能标记退款成功并返回 COMMON_400")
    void markRefundSuccessShouldRejectInvalidStatus() {
        // Given：售后单尚未进入退款处理中。
        AftersaleOrder pending = sampleOrder();

        // When
        Throwable throwable = catchThrowable(() -> pending.markRefundSuccess(LocalDateTime.now()));

        // Then：退款状态机不允许跳跃式流转。
        assertBadRequest(throwable, "当前售后单状态不允许标记退款成功");
    }

    @Test
    @DisplayName("边界值：售后单不存在时返回 COMMON_400")
    void loadByAftersaleNoShouldRejectMissingOrder() {
        // Given：仓储未查询到售后单。
        when(aftersaleOrderRepository.findByAftersaleNo("AS100")).thenReturn(Optional.empty());

        // When
        Throwable throwable = catchThrowable(() -> aftersaleDomainService.loadByAftersaleNo("AS100"));

        // Then：不存在异常明确返回给调用方。
        assertBadRequest(throwable, "售后单不存在");
    }

    @Test
    @DisplayName("并发风险场景：重复审核已通过售后单时拒绝二次审核并返回 COMMON_400")
    void approveShouldRejectRepeatedReview() {
        // Given：售后单已审核通过。
        AftersaleOrder approved = sampleOrder().approve(LocalDateTime.now());

        // When
        Throwable throwable = catchThrowable(() -> approved.approve(LocalDateTime.now().plusSeconds(1)));

        // Then：重复审核被状态机拦截。
        assertBadRequest(throwable, "当前售后单状态不允许审核通过");
    }

    private AftersaleOrder sampleOrder() {
        return AftersaleOrder.createOnlyRefund("AS100", "ORD100", 1L, "PAID", 1000L, "不想要了", LocalDateTime.now());
    }
}
