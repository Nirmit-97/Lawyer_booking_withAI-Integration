package com.legalconnect.lawyerbooking.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import com.legalconnect.lawyerbooking.util.JwtUtil;
import com.legalconnect.lawyerbooking.security.UserPrincipal;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private com.legalconnect.lawyerbooking.security.WebSocketAuthenticationInterceptor authInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WebSocketConfig.class);

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new org.springframework.web.socket.server.HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request, 
                                                 org.springframework.http.server.ServerHttpResponse response, 
                                                 org.springframework.web.socket.WebSocketHandler wsHandler, 
                                                 java.util.Map<String, Object> attributes) throws Exception {
                        logger.info("DEBUG Handshake: Request URI: {}", request.getURI());
                        logger.info("DEBUG Handshake: Headers: {}", request.getHeaders());
                        
                        String authToken = request.getHeaders().getFirst("Authorization");
                        if (authToken == null) {
                            // Also try query parameter 'token' as fallback for some clients
                            String query = request.getURI().getQuery();
                            if (query != null && query.contains("token=")) {
                                authToken = "Bearer " + query.split("token=")[1].split("&")[0];
                                logger.info("DEBUG Handshake: Found token in query string");
                            }
                        }
                        
                        if (authToken != null) {
                            logger.info("DEBUG Handshake: Storing Authorization in attributes");
                            attributes.put("Authorization", authToken);
                        } else {
                            logger.warn("DEBUG Handshake: No Authorization found in headers or query");
                        }
                        return true;
                    }

                    @Override
                    public void afterHandshake(org.springframework.http.server.ServerHttpRequest request, 
                                              org.springframework.http.server.ServerHttpResponse response, 
                                              org.springframework.web.socket.WebSocketHandler wsHandler, 
                                              Exception exception) {}
                })
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}
