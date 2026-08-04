package com.carrepair.websocket;

import com.alibaba.fastjson2.JSON;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 维修进度WebSocket处理器
 */
@Component
public class RepairWebSocketHandler extends TextWebSocketHandler {

    // 存储用户连接 userId -> session
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = extractUserId(session);
        if (userId != null) {
            sessions.put(userId, session);
            System.out.println("WebSocket连接建立: userId=" + userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Map<String, Object> msg = JSON.parseObject(payload, Map.class);
        
        // 处理心跳
        if ("PING".equals(msg.get("type"))) {
            session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = extractUserId(session);
        if (userId != null) {
            sessions.remove(userId);
            System.out.println("WebSocket连接关闭: userId=" + userId);
        }
    }

    /**
     * 向指定用户发送消息
     */
    public void sendToUser(String userId, Object message) {
        System.out.println("WebSocket尝试发送消息给用户: " + userId);
        WebSocketSession session = sessions.get(userId);
        
        if (session == null) {
            System.out.println("警告: 用户 " + userId + " 的WebSocket连接不存在");
            System.out.println("当前活跃连接数: " + sessions.size());
            System.out.println("活跃用户ID列表: " + sessions.keySet());
            return;
        }
        
        if (!session.isOpen()) {
            System.out.println("警告: 用户 " + userId + " 的WebSocket连接已关闭");
            sessions.remove(userId);
            return;
        }
        
        try {
            String jsonMessage = JSON.toJSONString(message);
            session.sendMessage(new TextMessage(jsonMessage));
            System.out.println("成功发送WebSocket消息给用户 " + userId + ": " + jsonMessage);
        } catch (IOException e) {
            System.out.println("发送WebSocket消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从URI中提取userId
     */
    private String extractUserId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return null;
    }
}
