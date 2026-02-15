package com.portfolio.monitor.config;

import com.portfolio.monitor.service.EventTrackingService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketEventListener {

    private final EventTrackingService eventTrackingService;
    private final Set<String> activeSessions = ConcurrentHashMap.newKeySet();

    public WebSocketEventListener(EventTrackingService eventTrackingService) {
        this.eventTrackingService = eventTrackingService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            activeSessions.add(sessionId);
            eventTrackingService.onUserConnected(sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId != null) {
            activeSessions.remove(sessionId);
            eventTrackingService.onUserDisconnected(sessionId);
        }
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}
