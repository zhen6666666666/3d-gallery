package com.example.artwork_3d_frontend.config;

import com.example.artwork_3d_frontend.websocket.ExhibitionWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ExhibitionWebSocketHandler exhibitionWebSocketHandler;

    public WebSocketConfig(ExhibitionWebSocketHandler exhibitionWebSocketHandler) {
        this.exhibitionWebSocketHandler = exhibitionWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(exhibitionWebSocketHandler, "/ws/exhibition")
                .setAllowedOriginPatterns("*"); // 允许跨域连接，解决 403 握手问题
    }
}