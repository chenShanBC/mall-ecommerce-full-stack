package com.mallfei.file.domain.service;

import com.mallfei.file.config.FileStorageProperties;
import com.mallfei.testsupport.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("mall-file 文件领域服务纯单元测试")
class FileDomainServiceTest extends BaseUnitTest {

    @Mock
    private FileStorageProperties fileStorageProperties;

    @InjectMocks
    private FileDomainService fileDomainService;

    @Test
    @DisplayName("正向业务流程：合法 PNG 头像文件校验通过并返回扩展名")
    void validateAvatarFileShouldAcceptValidPng() {
        // Given：上传文件扩展名、Content-Type、大小均合法。
        when(fileStorageProperties.getMaxAvatarSize()).thenReturn(2 * 1024 * 1024L);
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        // When
        String extension = fileDomainService.validateAvatarFile(file);

        // Then
        assertThat(extension).isEqualTo("png");
    }

    @Test
    @DisplayName("边界值：文件大小超过限制时拒绝上传并返回 COMMON_400")
    void validateProductImageFileShouldRejectOversizedFile() {
        // Given：商品图片超过配置大小上限。
        when(fileStorageProperties.getMaxAvatarSize()).thenReturn(1L);
        MockMultipartFile file = new MockMultipartFile("file", "product.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2});

        // When
        Throwable throwable = catchThrowable(() -> fileDomainService.validateProductImageFile(file));

        // Then
        assertBadRequest(throwable, "商品图片不能超过2MB");
    }

    @Test
    @DisplayName("异常场景：伪装成图片但 Content-Type 非图片时拒绝上传并返回 COMMON_400")
    void validateAvatarFileShouldRejectInvalidContentType() {
        // Given：文件扩展名看似图片，但 Content-Type 为 text/plain。
        when(fileStorageProperties.getMaxAvatarSize()).thenReturn(2 * 1024 * 1024L);
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", MediaType.TEXT_PLAIN_VALUE, new byte[]{1});

        // When
        Throwable throwable = catchThrowable(() -> fileDomainService.validateAvatarFile(file));

        // Then：防止伪造扩展名绕过文件类型校验。
        assertBadRequest(throwable, "头像文件类型不支持");
    }

    @Test
    @DisplayName("工具类单元测试：文件扩展名解析统一转小写")
    void resolveExtensionShouldLowerCaseSuffix() {
        // Given / When / Then
        assertThat(fileDomainService.resolveExtension("PRODUCT.WEBP")).isEqualTo("webp");
    }

    @Test
    @DisplayName("异常场景：无扩展名文件名不合法并返回 COMMON_400")
    void resolveExtensionShouldRejectMissingSuffix() {
        // Given：文件名没有扩展名。
        String filename = "avatar";

        // When
        Throwable throwable = catchThrowable(() -> fileDomainService.resolveExtension(filename));

        // Then
        assertBadRequest(throwable, "文件名不合法");
    }
}
