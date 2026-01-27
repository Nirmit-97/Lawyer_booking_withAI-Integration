package com.legalconnect.lawyerbooking.security;

import com.legalconnect.lawyerbooking.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Interceptor to authenticate WebSocket connections using JWT
 */
@Component
public class WebSocketAuthenticationInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthenticationInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authToken = accessor.getFirstNativeHeader("Authorization");
            logger.info("WebSocket CONNECT attempt. Authorization header present: {}", (authToken != null));

            if (authToken != null && authToken.startsWith("Bearer ")) {
                String jwt = authToken.substring(7);
                try {
                    String username = jwtUtil.extractUsername(jwt);
                    String role = jwtUtil.extractRole(jwt);
                    Long userId = jwtUtil.extractUserId(jwt);

                    if (username != null && jwtUtil.validateToken(jwt, username)) {
                        logger.info("WebSocket Authenticated: {} [Role: {}]", username, role);
                        
                        UserPrincipal principal = new UserPrincipal(userId, username, role);
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                        
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                principal, null, Collections.singletonList(authority));
                        
                        // Set the user in the accessor so it's available in the session
                        accessor.setUser(auth);
                        
                        // Also set in SecurityContextHolder for the current thread
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } else {
                        logger.warn("WebSocket Auth Failed: Invalid token for user {}", username);
                    }
                } catch (Exception e) {
                    logger.error("WebSocket Auth Error: {}", e.getMessage());
                }
            } else {
                logger.warn("WebSocket CONNECT without Bearer token");
            }
        }

        return message;
    }
}
