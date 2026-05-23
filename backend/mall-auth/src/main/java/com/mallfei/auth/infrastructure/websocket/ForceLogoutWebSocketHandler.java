package com.mallfei.auth.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ForceLogoutWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Map<String, Set<WebSocketSession>> sessionsByPrincipal = new ConcurrentHashMap<>();

    public ForceLogoutWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String principalKey = resolvePrincipalKey(session);
        if (principalKey == null) {
            closeQuietly(session);
            return;
        }
        session.getAttributes().put("principalKey", principalKey);
        sessionsByPrincipal.computeIfAbsent(principalKey, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String principalKey = (String) session.getAttributes().get("principalKey");
        if (principalKey == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionsByPrincipal.get(principalKey);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByPrincipal.remove(principalKey);
        }
    }

    public void forceLogout(Long userId, String message) {
        forceLogout("USER", userId, message);
    }

    public void forceLogoutAdmin(Long adminId, String message) {
        forceLogout("ADMIN", adminId, message);
    }

    private void forceLogout(String identityCode, Long principalId, String message) {
        Set<WebSocketSession> sessions = sessionsByPrincipal.get(principalKey(identityCode, principalId));
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        String payload = writePayload(message);
        sessions.removeIf(session -> {
            if (!session.isOpen()) {
                return true;
            }
            try {
                session.sendMessage(new TextMessage(payload));
                return false;
            } catch (IOException e) {
                closeQuietly(session);
                return true;
            }
        });
        if (sessions.isEmpty()) {
            sessionsByPrincipal.remove(principalKey(identityCode, principalId));
        }
    }

    private String resolvePrincipalKey(WebSocketSession session) {
        Object identityType = session.getAttributes().get("identityType");
        Object principalId = session.getAttributes().get("principalId");
        if (identityType == null || principalId == null) {
            return null;
        }
        return principalKey(String.valueOf(identityType), principalId);
    }

    private String principalKey(String identityCode, Object principalId) {
        return identityCode + ":" + principalId;
    }

    private String writePayload(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "type", "forceLogout",
                    "message", Objects.requireNonNullElse(message, "您已被平台禁用，即将退出登录")
            ));
        } catch (Exception e) {
            return "{\"type\":\"forceLogout\",\"message\":\"您已被平台禁用，即将退出登录\"}";
        }
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.POLICY_VIOLATION);
            }
        } catch (IOException ignored) {
        }
    }
}
