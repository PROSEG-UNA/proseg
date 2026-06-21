package com.sssi.msvc_maintenance.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class TicketWebSocketManager {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TicketWebSocketManager.class);

    private final ObjectMapper objectMapper;

    private final Set<WebSocketSession> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void register(WebSocketSession session) {
        sessions.add(session);
    }

    public void unregister(WebSocketSession session) {
        sessions.remove(session);
    }

    public void broadcast(TicketWebSocketEventDto event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            TextMessage msg = new TextMessage(payload);
            for (WebSocketSession s : sessions) {
                try {
                    if (s.isOpen()) {
                        s.sendMessage(msg);
                    } else {
                        sessions.remove(s);
                    }
                } catch (Exception ex) {
                    sessions.remove(s);
                    log.warn("Failed to send ticket websocket event", ex);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to serialize ticket websocket event", ex);
        }
    }
}
