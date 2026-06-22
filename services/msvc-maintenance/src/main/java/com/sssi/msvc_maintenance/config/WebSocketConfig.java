package com.sssi.msvc_maintenance.config;

import com.sssi.msvc_maintenance.websocket.TicketWebSocketHandler;
import com.sssi.msvc_maintenance.websocket.TicketWebSocketManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final TicketWebSocketManager ticketWebSocketManager;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new TicketWebSocketHandler(ticketWebSocketManager), "/api/v1/maintenance/ws/tickets")
                .setAllowedOriginPatterns("*");
    }
}
