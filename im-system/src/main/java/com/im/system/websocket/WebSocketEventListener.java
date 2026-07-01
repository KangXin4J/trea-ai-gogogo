package com.im.system.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final Map<String, Long> connectedUsers = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attributes = accessor.getSessionAttributes();

        if (attributes != null) {
            Long userId = (Long) attributes.get("userId");
            String username = (String) attributes.get("username");
            String sessionId = accessor.getSessionId();

            if (userId != null) {
                connectedUsers.put(sessionId, userId);
                log.info("用户连接: userId={}, username={}, sessionId={}", userId, username, sessionId);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        Long userId = connectedUsers.remove(sessionId);
        if (userId != null) {
            log.info("用户断开连接: userId={}, sessionId={}", userId, sessionId);
        }
    }

    public int getOnlineUserCount() {
        return connectedUsers.size();
    }
}
