package com.mallfei.auth.infrastructure.websocket;

import cn.dev33.satoken.stp.StpUtil;
import com.mallfei.common.enums.IdentityType;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Configuration
@EnableWebSocket
public class ForceLogoutWebSocketConfig implements WebSocketConfigurer {

    private final ForceLogoutWebSocketHandler forceLogoutWebSocketHandler;

    public ForceLogoutWebSocketConfig(ForceLogoutWebSocketHandler forceLogoutWebSocketHandler) {
        this.forceLogoutWebSocketHandler = forceLogoutWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(forceLogoutWebSocketHandler, "/ws/force-logout")
                .addInterceptors(new TokenHandshakeInterceptor(IdentityType.USER))
                .setAllowedOriginPatterns("*");
        registry.addHandler(forceLogoutWebSocketHandler, "/ws/admin/force-logout")
                .addInterceptors(new TokenHandshakeInterceptor(IdentityType.ADMIN))
                .setAllowedOriginPatterns("*");
    }

    private static class TokenHandshakeInterceptor implements HandshakeInterceptor {

        private final IdentityType identityType;

        private TokenHandshakeInterceptor(IdentityType identityType) {
            this.identityType = identityType;
        }

        @Override
        public boolean beforeHandshake(ServerHttpRequest request,
                                       ServerHttpResponse response,
                                       WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) {
            String token = tokenFromQuery(request.getURI()).orElse(null);
            if (token == null || token.isBlank()) {
                return false;
            }
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                return false;
            }
            String loginIdText = String.valueOf(loginId);
            String prefix = identityType.code() + ":";
            if (!loginIdText.startsWith(prefix)) {
                return false;
            }
            attributes.put("identityType", identityType.code());
            attributes.put("principalId", Long.parseLong(loginIdText.substring(prefix.length())));
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Exception exception) {
        }

        private Optional<String> tokenFromQuery(URI uri) {
            String query = uri.getRawQuery();
            if (query == null || query.isBlank()) {
                return Optional.empty();
            }
            return Arrays.stream(query.split("&"))
                    .map(item -> item.split("=", 2))
                    .filter(parts -> parts.length == 2 && "token".equals(parts[0]))
                    .map(parts -> parts[1])
                    .findFirst();
        }
    }
}
