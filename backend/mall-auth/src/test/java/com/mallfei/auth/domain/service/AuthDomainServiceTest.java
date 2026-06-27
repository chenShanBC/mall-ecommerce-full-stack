package com.mallfei.auth.domain.service;

import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.enums.IdentityType;
import com.mallfei.testsupport.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("mall-auth 授权领域服务纯单元测试")
class AuthDomainServiceTest extends BaseUnitTest {

    private final AuthDomainService authDomainService = new AuthDomainService();

    @Test
    @DisplayName("JWT签发校验：已解析出登录主体时视为 Token 有效")
    void ensureLoggedInShouldAcceptResolvedJwtPrincipal() {
        // Given：网关/拦截器已完成 Token 解析，并透传统一登录主体。
        AuthenticatedPrincipal principal = userPrincipal(1L);

        // When & Then：领域服务只校验主体存在，不启动 Spring 容器、不依赖 Redis。
        assertThatCode(() -> authDomainService.ensureLoggedIn(principal)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("JWT签发校验异常：Token 缺失或解析失败时拒绝访问并返回 AUTH_403")
    void ensureLoggedInShouldRejectMissingPrincipal() {
        // Given：请求链路未解析到登录主体，等价于未登录/Token 无效。
        AuthenticatedPrincipal principal = null;

        // When
        Throwable throwable = catchThrowable(() -> authDomainService.ensureLoggedIn(principal));

        // Then：权限类异常必须同时覆盖类型、错误码和核心文案。
        assertForbidden(throwable, "当前未登录");
    }

    @Test
    @DisplayName("授权边界：身份类型不匹配时拒绝访问用户资源并返回 AUTH_403")
    void ensureIdentityShouldRejectWrongIdentityType() {
        // Given：管理员身份访问 C 端用户资源。
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal("ADMIN:1", 1L, "admin", IdentityType.ADMIN, "管理员", "avatar.png", "token", "ADMIN", List.of());

        // When
        Throwable throwable = catchThrowable(() -> authDomainService.ensureIdentity(principal, IdentityType.USER, "仅用户可访问"));

        // Then：权限类异常统一归口 AUTH_403，便于前端与网关做一致处理。
        assertForbidden(throwable, "仅用户可访问");
    }

    @Test
    @DisplayName("授权正向：身份类型匹配时允许继续访问")
    void ensureIdentityShouldAcceptMatchedIdentityType() {
        // Given：C 端用户访问用户资源。
        AuthenticatedPrincipal principal = userPrincipal(2L);

        // When & Then
        assertThatCode(() -> authDomainService.ensureIdentity(principal, IdentityType.USER, "仅用户可访问"))
                .doesNotThrowAnyException();
    }

    private AuthenticatedPrincipal userPrincipal(Long userId) {
        return new AuthenticatedPrincipal("USER:" + userId, userId, "user" + userId, IdentityType.USER, "用户" + userId, "avatar.png", "token", "USER", List.of());
    }
}
