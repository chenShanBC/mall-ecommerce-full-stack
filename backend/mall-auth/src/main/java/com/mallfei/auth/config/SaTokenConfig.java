package com.mallfei.auth.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.mallfei.common.auth.RequireAdmin;
import com.mallfei.common.auth.RequireLogin;
import com.mallfei.common.auth.RequirePermission;
import com.mallfei.common.auth.RequireUser;
import com.mallfei.common.enums.IdentityType;
import com.mallfei.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
                    String requestUri = SaHolder.getRequest().getRequestPath();
                    String method = SaHolder.getRequest().getMethod();
                    if ("OPTIONS".equalsIgnoreCase(method) || isPublicPath(requestUri)) {
                        return;
                    }
                    StpUtil.checkLogin();
                }))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/error",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );

        registry.addInterceptor(new AuthorizationAnnotationInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/error",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(false)
                .maxAge(3600);
    }

    private boolean isPublicPath(String requestUri) {
        return requestUri.startsWith("/uploads/")
                || requestUri.startsWith("/api/users/register")
                || requestUri.startsWith("/api/users/login/captcha/challenge")
                || requestUri.startsWith("/api/users/login/captcha/verify")
                || requestUri.startsWith("/api/users/login/password")
                || requestUri.startsWith("/api/users/login/sms/send-code")
                || requestUri.startsWith("/api/users/login/sms")
                || requestUri.startsWith("/api/admin/login/password")
                || requestUri.startsWith("/api/products")
                || requestUri.startsWith("/api/categories")
                || requestUri.startsWith("/api/pay/alipay/return-bridge")
                || requestUri.matches("^/api/pay/orders/[^/]+/submit-page$")
                || requestUri.startsWith("/api/pay/callback");
    }

    static class AuthorizationAnnotationInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            if (!(handler instanceof HandlerMethod handlerMethod)) {
                return true;
            }
            boolean needLogin = hasRequireLogin(handlerMethod) || hasRequireAdmin(handlerMethod) || hasRequireUser(handlerMethod) || hasRequirePermission(handlerMethod);
            if (!needLogin) {
                return true;
            }
            try {
                StpUtil.checkLogin();
            } catch (NotLoginException exception) {
                throw exception;
            }
            IdentityType identityType = IdentityType.fromCode(StpUtil.getSession().getString("identityType"));
            if (hasRequireAdmin(handlerMethod) && !identityType.isAdmin()) {
                throw BusinessException.forbidden("仅管理员可访问当前接口");
            }
            if (hasRequireUser(handlerMethod) && identityType != IdentityType.USER) {
                throw BusinessException.forbidden("仅用户可访问当前接口");
            }
            List<String> requiredPermissions = requiredPermissions(handlerMethod);
            if (!requiredPermissions.isEmpty()) {
                String permissionsJson = StpUtil.getSession().getString("permissions");
                if (permissionsJson == null) {
                    throw BusinessException.forbidden("当前账号暂无访问权限");
                }
                boolean hasPermission = Arrays.stream(permissionsJson.replace("[", "").replace("]", "").replace("\"", "").split(","))
                        .map(String::trim)
                        .anyMatch(requiredPermissions::contains);
                if (!hasPermission) {
                    throw BusinessException.forbidden("当前账号无权访问该功能模块");
                }
            }
            return true;
        }

        private boolean hasRequireLogin(HandlerMethod handlerMethod) {
            return AnnotationUtils.findAnnotation(handlerMethod.getMethod(), RequireLogin.class) != null
                    || AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequireLogin.class) != null;
        }

        private boolean hasRequireAdmin(HandlerMethod handlerMethod) {
            return AnnotationUtils.findAnnotation(handlerMethod.getMethod(), RequireAdmin.class) != null
                    || AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequireAdmin.class) != null;
        }

        private boolean hasRequireUser(HandlerMethod handlerMethod) {
            return AnnotationUtils.findAnnotation(handlerMethod.getMethod(), RequireUser.class) != null
                    || AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequireUser.class) != null;
        }

        private boolean hasRequirePermission(HandlerMethod handlerMethod) {
            return AnnotationUtils.findAnnotation(handlerMethod.getMethod(), RequirePermission.class) != null
                    || AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequirePermission.class) != null;
        }

        private List<String> requiredPermissions(HandlerMethod handlerMethod) {
            RequirePermission onMethod = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), RequirePermission.class);
            if (onMethod != null) {
                return List.of(onMethod.value());
            }
            RequirePermission onType = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequirePermission.class);
            if (onType != null) {
                return List.of(onType.value());
            }
            return List.of();
        }
    }
}
