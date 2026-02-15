package com.portfolio.monitor.controller;

import com.portfolio.monitor.dto.CursorEvent;
import com.portfolio.monitor.dto.DrawEvent;
import com.portfolio.monitor.service.EventTrackingService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WhiteboardController {

    private final EventTrackingService eventTrackingService;

    public WhiteboardController(EventTrackingService eventTrackingService) {
        this.eventTrackingService = eventTrackingService;
    }

    @MessageMapping("/whiteboard/{boardId}/draw")
    @SendTo("/topic/whiteboard/{boardId}")
    public DrawEvent handleDraw(@DestinationVariable String boardId, DrawEvent event) {
        eventTrackingService.onDraw(event.getSenderId(), event.getTool(), boardId);
        return event;
    }

    @MessageMapping("/whiteboard/{boardId}/cursor")
    @SendTo("/topic/whiteboard/{boardId}/cursors")
    public CursorEvent handleCursor(@DestinationVariable String boardId, CursorEvent event) {
        eventTrackingService.onCursorMove(event.getNickname(), boardId);
        return event;
    }

    @MessageMapping("/whiteboard/{boardId}/clear")
    @SendTo("/topic/whiteboard/{boardId}")
    public DrawEvent handleClear(@DestinationVariable String boardId, DrawEvent event) {
        event.setAction("CLEAR");
        eventTrackingService.onClear(event.getSenderId(), boardId);
        return event;
    }
}
