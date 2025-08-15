package com.nhom4.xoxo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.nhom4.xoxo.security.CustomUserDetailsService;
import com.nhom4.xoxo.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

	private final JwtTokenProvider jwtTokenProvider;
	private final CustomUserDetailsService customUserDetailsService;

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new ChannelInterceptor() {
			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

				if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
					String token = accessor.getFirstNativeHeader("Authorization");

					if (token != null && token.startsWith("Bearer ")) {
						token = token.substring(7);
						try {
							if (jwtTokenProvider.validateToken(token)) {
								String username = jwtTokenProvider.getUsernameFromJWT(token);
								UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
								Authentication auth = new UsernamePasswordAuthenticationToken(
									userDetails, null, userDetails.getAuthorities()
								);
								accessor.setUser(auth); // principal cho phiên STOMP
								SecurityContextHolder.getContext().setAuthentication(auth); // tùy chọn
								log.info("WebSocket authenticated for user: {}", username);
							}
						} catch (Exception e) {
							log.error("WebSocket authentication failed: {}", e.getMessage());
						}
					}
				}
				return message;
			}
		});
	}
}