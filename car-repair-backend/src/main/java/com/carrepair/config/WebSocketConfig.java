package com.carrepair.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.carrepair.websocket.RepairWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * WebSocket配置
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private RepairWebSocketHandler repairWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(repairWebSocketHandler, "/ws/{userId}")
                .setAllowedOrigins("*");
    }
}
