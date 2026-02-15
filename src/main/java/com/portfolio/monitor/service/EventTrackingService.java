package com.portfolio.monitor.service;

import com.portfolio.monitor.dto.EventLog;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class EventTrackingService {

    private final SimpMessagingTemplate messagingTemplate;
    private final AtomicLong totalDrawEvents = new AtomicLong(0);
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final Map<String, Long> lastDrawLogTime = new ConcurrentHashMap<>();
    private static final long DRAW_LOG_THROTTLE_MS = 3000;

    public EventTrackingService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void pushEvent(String type, String message, String level) {
        totalMessages.incrementAndGet();
        EventLog log = new EventLog(
                UUID.randomUUID().toString().substring(0, 8),
                type,
                message,
                level,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        );
        messagingTemplate.convertAndSend("/topic/events", log);
    }

    public void onUserConnected(String sessionId) {
        pushEvent("CONNECT", "새 사용자 접속 [" + sessionId.substring(0, 8) + "]", "INFO");
    }

    public void onUserDisconnected(String sessionId) {
        pushEvent("CONNECT", "사용자 퇴장 [" + sessionId.substring(0, 8) + "]", "INFO");
    }

    public void onDraw(String senderId, String tool, String boardId) {
        totalDrawEvents.incrementAndGet();
        totalMessages.incrementAndGet();
        long now = System.currentTimeMillis();
        Long last = lastDrawLogTime.get(senderId);
        if (last == null || now - last >= DRAW_LOG_THROTTLE_MS) {
            lastDrawLogTime.put(senderId, now);
            pushEvent("DRAW", senderId + "님이 보드 #" + boardId + "에서 " + toolName(tool), "INFO");
        }
    }

    public void onClear(String senderId, String boardId) {
        pushEvent("DRAW", senderId + "님이 보드 #" + boardId + " 전체 지우기 실행", "WARN");
    }

    public void onCursorMove(String nickname, String boardId) {
        totalMessages.incrementAndGet();
    }

    public long getTotalDrawEvents() {
        return totalDrawEvents.get();
    }

    public long getTotalMessages() {
        return totalMessages.get();
    }

    private String toolName(String tool) {
        return switch (tool) {
            case "pen" -> "펜 그리기";
            case "eraser" -> "지우개 사용";
            case "rect" -> "사각형 그리기";
            case "circle" -> "원 그리기";
            default -> "그리기";
        };
    }
}
