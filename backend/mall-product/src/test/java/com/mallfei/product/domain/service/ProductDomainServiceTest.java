package com.mallfei.product.domain.service;

import com.mallfei.product.application.dto.AdminCreateProductRequest;
import com.mallfei.product.application.dto.AdminUpdateProductRequest;
import com.mallfei.product.domain.model.Category;
import com.mallfei.product.domain.model.ProductSku;
import com.mallfei.product.domain.model.ProductSpu;
import com.mallfei.product.domain.repository.CategoryRepository;
import com.mallfei.product.domain.repository.ProductRepository;
import com.mallfei.testsupport.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("mall-product 商品领域服务纯单元测试")
class ProductDomainServiceTest extends BaseUnitTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductDomainService productDomainService;

    @Test
    @DisplayName("正向业务流程：创建一级类目时修剪名称并默认启用")
    void createCategoryShouldTrimNameAndEnableByDefault() {
        // Given：类目名称包含前后空格，且当前不存在重名类目。
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Category category = productDomainService.createCategory("  手机数码  ", 0L, 1);

        // Then：类目名称被规范化，且仓储保存动作被明确验证。
        assertThat(category.name()).isEqualTo("手机数码");
        assertThat(category.status()).isEqualTo("ENABLED");
        verify(categoryRepository).save(argThat(saved -> "手机数码".equals(saved.name()) && "ENABLED".equals(saved.status())));
    }

    @Test
    @DisplayName("边界值：类目名称超过 64 个字符时拒绝创建并返回 COMMON_400")
    void createCategoryShouldRejectTooLongName() {
        // Given：类目名称超过平台约定最大长度。
        String longName = "a".repeat(65);

        // When
        Throwable throwable = catchThrowable(() -> productDomainService.createCategory(longName, 0L, 1));

        // Then：参数异常必须断言错误码，且不得落库。
        assertBadRequest(throwable, "类目名称不能超过64个字符");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("异常场景：同名类目已存在时拒绝创建并返回 COMMON_400")
    void createCategoryShouldRejectDuplicateName() {
        // Given：同级业务上已存在同名类目。
        when(categoryRepository.findAll()).thenReturn(List.of(new Category(1L, "手机数码", 0L, 1, 1, "ENABLED")));

        // When
        Throwable throwable = catchThrowable(() -> productDomainService.createCategory("手机数码", 0L, 1));

        // Then：冲突在领域层被拦截，不触发保存。
        assertBadRequest(throwable, "分类名称已存在");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("正向业务流程：创建商品时校验 SKU 与初始库存并保存上架商品")
    void createProductShouldBuildOnlineSpu() {
        // Given：类目存在，商品请求包含合法 SKU 与初始库存。
        when(productRepository.save(any(ProductSpu.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AdminCreateProductRequest request = new AdminCreateProductRequest(
                "测试商品",
                1L,
                "https://img.example.com/a.png",
                "desc",
                List.of(new AdminCreateProductRequest.SkuInput("SKU-1", "SKU名称", "{}", 1999L, 2999L, 10))
        );

        // When
        ProductSpu spu = productDomainService.createProduct(request);

        // Then：生成 ONLINE 商品并持久化，SKU 金额保持分单位精度。
        assertThat(spu.status()).isEqualTo("ONLINE");
        assertThat(spu.skus()).hasSize(1);
        assertThat(spu.skus().getFirst().salePriceCent()).isEqualTo(1999L);
        verify(productRepository).save(argThat(saved -> "测试商品".equals(saved.name()) && saved.skus().size() == 1));
    }

    @Test
    @DisplayName("异常场景：商品上架时不允许新增 SKU 并返回 COMMON_400")
    void updateProductShouldRejectNewSkuWhenOnline() {
        // Given：商品处于上架状态，更新请求试图新增 SKU。
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleOnlineSpu()));
        AdminUpdateProductRequest request = new AdminUpdateProductRequest(
                "测试商品",
                1L,
                "https://img.example.com/a.png",
                "desc",
                "ONLINE",
                List.of(new AdminUpdateProductRequest.SkuInput(null, "SKU-NEW", "新SKU", "{}", 1999L, 2999L, "ONLINE", 10))
        );

        // When
        Throwable throwable = catchThrowable(() -> productDomainService.updateProduct(1L, request));

        // Then：上架商品 SKU 结构保护生效，不执行保存。
        assertBadRequest(throwable, "商品上架状态不允许新增SKU及初始库存");
        verify(productRepository, never()).save(any());
    }

    private ProductSpu sampleOnlineSpu() {
        return ProductSpu.create(1L, "测试商品", 1L, "https://img.example.com/a.png", "[]", "desc", "ONLINE", List.of(new ProductSku(1L, 1L, "SKU-1", "SKU名称", "{}", 1999L, 2999L, 0, "ONLINE")));
    }
}
