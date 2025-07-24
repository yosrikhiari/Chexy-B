package org.example.chessmystic.Config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        // Extract token from query parameters if present
        URI uri = request.getURI();
        String query = uri.getQuery();
        String token = null;

        if (query != null && query.contains("token=")) {
            token = UriComponentsBuilder.fromUriString(uri.toString())
                    .build()
                    .getQueryParams()
                    .getFirst("token");
        }

        // Also check Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        if (token != null && !token.isEmpty()) {
            // Store token in WebSocket session attributes for later use
            attributes.put("jwt_token", token);
            System.out.println("[WebSocket] Token extracted and stored in session");
            return true;
        }

        System.out.println("[WebSocket] No valid token found, allowing connection anyway");
        // For now, allow connection even without token - you can make this stricter later
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {

        if (exception != null) {
            System.err.println("[WebSocket] Handshake failed: " + exception.getMessage());
        } else {
            System.out.println("[WebSocket] Handshake completed successfully");
        }
    }
}