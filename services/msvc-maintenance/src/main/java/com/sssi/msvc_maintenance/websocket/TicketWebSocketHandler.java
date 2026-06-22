package com.sssi.msvc_maintenance.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@RequiredArgsConstructor
public class TicketWebSocketHandler extends TextWebSocketHandler {

    private final TicketWebSocketManager ticketWebSocketManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        ticketWebSocketManager.register(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        ticketWebSocketManager.unregister(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // no-op (server pushes events)
    }
}
