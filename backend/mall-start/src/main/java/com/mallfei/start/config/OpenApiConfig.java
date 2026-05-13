package com.mallfei.start.config;

import com.mallfei.common.auth.RequireAdmin;
import com.mallfei.common.auth.RequireLogin;
import com.mallfei.common.auth.RequireUser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;

import java.lang.annotation.Annotation;
import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String AUTHORIZATION_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI mallFeiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("mallFei API")
                        .version("v1.0.0")
                        .description("mallFei 接口文档"))
                .components(new Components()
                        .addSecuritySchemes(AUTHORIZATION_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("填写登录接口返回的 token，Swagger 会自动以 Bearer 前缀放入 Authorization 请求头")));
    }

    @Bean
    public OperationCustomizer authorizationOperationCustomizer() {
        return (operation, handlerMethod) -> {
            if (requiresAuthorization(handlerMethod)) {
                operation.setSecurity(List.of(new SecurityRequirement().addList(AUTHORIZATION_SCHEME)));
            }
            return operation;
        };
    }

    private boolean requiresAuthorization(HandlerMethod handlerMethod) {
        return hasAnnotation(handlerMethod, RequireLogin.class)
                || hasAnnotation(handlerMethod, RequireAdmin.class)
                || hasAnnotation(handlerMethod, RequireUser.class);
    }

    private boolean hasAnnotation(HandlerMethod handlerMethod,
                                  Class<? extends Annotation> annotationType) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), annotationType)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), annotationType);
    }
}
