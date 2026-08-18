package com.example.artwork_3d_frontend.websocket;

import com.example.artwork_3d_frontend.dto.PlayerState;
import com.example.artwork_3d_frontend.dto.WsMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ExhibitionWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 存储当前在线的 Session 与用户 ID 映射
    private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();
    // 存储 Session ID 到 用户 ID 的映射，便于断开时快速查找
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    // 存储所有在线玩家的实时姿态数据 <userId, PlayerState>
    private final Map<Long, PlayerState> onlinePlayers = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionMap.put(session.getId(), session);
        log.info("WebSocket 连接建立, SessionID: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WsMessage wsMsg = objectMapper.readValue(message.getPayload(), WsMessage.class);
        String type = wsMsg.getType();

        switch (type) {
            case "USER_JOIN":
                handleUserJoin(session, wsMsg);
                break;
            case "USER_MOVE":
                handleUserMove(wsMsg);
                break;
            default:
                break;
        }
    }

    /**
     * 处理用户加入展厅
     */
    private void handleUserJoin(WebSocketSession session, WsMessage wsMsg) throws IOException {
        PlayerState playerState = objectMapper.convertValue(wsMsg.getData(), PlayerState.class);
        Long userId = playerState.getUserId();

        sessionUserMap.put(session.getId(), userId);
        onlinePlayers.put(userId, playerState);

        // 核心修复：senderId 传 0L 代表系统服务器发送，避免被前端误判为“自己发的消息”而拦截[cite: 6]
        WsMessage initMsg = new WsMessage("INIT_USERS", 0L, onlinePlayers.values());
        sendMessageSync(session, initMsg);

        // 广播给其他人：有新玩家加入
        broadcastToOthers(userId, new WsMessage("USER_JOIN", userId, playerState));
        log.info("玩家加入展厅: userId={}, nickname={}", userId, playerState.getNickname());
    }

    /**
     * 处理用户移动/旋转姿态同步
     */
    private void handleUserMove(WsMessage wsMsg) throws IOException {
        PlayerState moveData = objectMapper.convertValue(wsMsg.getData(), PlayerState.class);
        Long userId = wsMsg.getSenderId();

        // 更新服务端的缓存状态
        PlayerState player = onlinePlayers.get(userId);
        if (player != null) {
            player.setX(moveData.getX());
            player.setY(moveData.getY());
            player.setZ(moveData.getZ());
            player.setRotationY(moveData.getRotationY());

            // 将最新坐标广播给除自己以外的其他用户
            broadcastToOthers(userId, new WsMessage("USER_MOVE", userId, player));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        Long userId = sessionUserMap.remove(sessionId);
        sessionMap.remove(sessionId);

        if (userId != null) {
            onlinePlayers.remove(userId);
            // 广播通知其他人该用户已离开，前端移除 3D 模型与头顶标签
            broadcastToOthers(userId, new WsMessage("USER_LEAVE", userId, userId));
            log.info("玩家离开展厅: userId={}", userId);
        }
    }

    /**
     * 广播给指定用户之外的所有其他在线客户端
     */
    private void broadcastToOthers(Long excludeUserId, WsMessage msg) throws IOException {
        for (Map.Entry<String, Long> entry : sessionUserMap.entrySet()) {
            Long userId = entry.getValue();
            if (!userId.equals(excludeUserId)) {
                WebSocketSession session = sessionMap.get(entry.getKey());
                if (session != null && session.isOpen()) {
                    sendMessageSync(session, msg);
                }
            }
        }
    }

    /**
     * 线程安全的 WebSocket 消息发送方法
     */
    private void sendMessageSync(WebSocketSession session, WsMessage msg) throws IOException {
        if (session != null && session.isOpen()) {
            synchronized (session) {
                String jsonPayload = objectMapper.writeValueAsString(msg);
                session.sendMessage(new TextMessage(jsonPayload));
            }
        }
    }
}