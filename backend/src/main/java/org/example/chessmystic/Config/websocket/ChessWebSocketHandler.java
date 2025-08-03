package org.example.chessmystic.Config.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChessWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(ChessWebSocketHandler.class);

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            sessionManager.addSession(session);
            log.info("WebSocket connection established: {}", session.getId());
        } catch (Exception e) {
            log.error("Error during connection establishment for session {}: {}",
                    session.getId(), e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            sessionManager.removeSession(session);
            log.info("WebSocket connection closed: {} - Status: {} Reason: {}",
                    session.getId(), status.getCode(), status.getReason());
        } catch (Exception e) {
            log.error("Error during connection cleanup for session {}: {}",
                    session.getId(), e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error for session {}: {}",
                session.getId(), exception.getMessage());
        try {
            sessionManager.removeSession(session);
        } catch (Exception e) {
            log.error("Error removing session after transport error: {}", e.getMessage());
        }
    }
}