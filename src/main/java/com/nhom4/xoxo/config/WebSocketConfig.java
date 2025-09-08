package com.nhom4.xoxo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for pub/sub messaging with heartbeat
        config.enableSimpleBroker("/topic", "/queue")
              .setHeartbeatValue(new long[]{10000, 10000}) // 10 second heartbeat
              .setTaskScheduler(taskScheduler());
        
        // Set application destination prefix for client-to-server messages
        config.setApplicationDestinationPrefixes("/app");
        
        // Set user destination prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoints with enhanced configuration
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js")
                .setHeartbeatTime(25000) // 25 seconds
                .setDisconnectDelay(5000); // 5 seconds
        
        // WebSocket endpoint without SockJS fallback - for modern browsers
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(new org.springframework.messaging.support.ChannelInterceptor() {
            @Override
            public org.springframework.messaging.Message<?> preSend(
                    org.springframework.messaging.Message<?> message, 
                    org.springframework.messaging.MessageChannel channel) {
                
                // Log WebSocket activity for monitoring
                org.springframework.messaging.simp.stomp.StompHeaderAccessor accessor = 
                    org.springframework.messaging.simp.stomp.StompHeaderAccessor.wrap(message);
                
                if (org.springframework.messaging.simp.stomp.StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Handle connection
                    String sessionId = accessor.getSessionId();
                    log.info("WebSocket connected: {}", sessionId);
                } else if (org.springframework.messaging.simp.stomp.StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                    // Handle disconnection
                    String sessionId = accessor.getSessionId();
                    log.info("WebSocket disconnected: {}", sessionId);
                }
                
                return message;
            }
        });
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.scheduling.TaskScheduler taskScheduler() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler scheduler = 
            new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("websocket-");
        scheduler.initialize();
        return scheduler;
    }
}
