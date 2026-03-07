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
        if (accessor == null) return message;

        logger.info("DEBUG Interceptor: Command={}, Dest={}, SessionAttr={}, NativeHeaders={}", 
                   accessor.getCommand(), 
                   accessor.getDestination(), 
                   accessor.getSessionAttributes(), 
                   accessor.toNativeHeaderMap());

        // 1. Check if already authenticated in this session or message
        if (accessor.getUser() != null) {
            return message;
        }

        // 2. Try to get token from multiple sources
        String authToken = accessor.getFirstNativeHeader("Authorization");
        
        // Fallback to session attributes (populated by HandshakeInterceptor)
        if (authToken == null && accessor.getSessionAttributes() != null) {
            authToken = (String) accessor.getSessionAttributes().get("Authorization");
        }

        if (authToken != null && authToken.startsWith("Bearer ")) {
            String jwt = authToken.substring(7);
            try {
                String username = jwtUtil.extractUsername(jwt);
                String role = jwtUtil.extractRole(jwt);
                Long userId = jwtUtil.extractUserId(jwt);

                if (username != null && jwtUtil.validateToken(jwt, username)) {
                    logger.info("WebSocket Authenticated: {} [Role: {}, Command: {}]", username, role, accessor.getCommand());
                    
                    UserPrincipal principal = new UserPrincipal(userId, username, role);
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                    
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            principal, null, Collections.singletonList(authority));
                    
                    // Set the user in the accessor
                    accessor.setUser(auth);
                    
                    // Also store in session attributes for subsequent frames if needed
                    if (accessor.getSessionAttributes() != null) {
                        accessor.getSessionAttributes().put("SPRING_SECURITY_AUTHENTICATION", auth);
                    }
                    
                    // Set in SecurityContextHolder for the current thread's interceptor chain
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    logger.warn("WebSocket Auth Failed: Invalid token for user {}", username);
                }
            } catch (Exception e) {
                logger.error("WebSocket Auth Error: {}", e.getMessage());
            }
        } else if (accessor.getSessionAttributes() != null) {
            // 3. Final fallback: recover previous authentication from session attributes
            org.springframework.security.core.Authentication auth = 
                (org.springframework.security.core.Authentication) accessor.getSessionAttributes().get("SPRING_SECURITY_AUTHENTICATION");
            if (auth != null) {
                accessor.setUser(auth);
            }
        }

        if (accessor.getUser() == null && !StompCommand.CONNECT.equals(accessor.getCommand()) && 
            !StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            logger.warn("WebSocket message without authentication - Command: {}, Dest: {}", 
                       accessor.getCommand(), accessor.getDestination());
        }

        return message;
    }
}
