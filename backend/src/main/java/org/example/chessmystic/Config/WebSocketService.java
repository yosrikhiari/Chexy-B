package org.example.chessmystic.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WebSocketService {
    private static final Logger log = LoggerFactory.getLogger(WebSocketService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;
    private final WebSocketSessionManager sessionManager;

    // Track failed message attempts per session
    private final ConcurrentMap<String, AtomicInteger> failureCount = new ConcurrentHashMap<>();
    private static final int MAX_FAILURES = 3;

    @Autowired
    public WebSocketService(SimpMessagingTemplate messagingTemplate,
                            SimpUserRegistry userRegistry,
                            WebSocketSessionManager sessionManager) {
        this.messagingTemplate = messagingTemplate;
        this.userRegistry = userRegistry;
        this.sessionManager = sessionManager;
    }

    /**
     * Send message to a specific user with enhanced error handling
     */
    public boolean sendToUser(String userId, String destination, Object message) {
        if (userId == null || destination == null || message == null) {
            log.warn("Invalid parameters for sendToUser: userId={}, destination={}, message={}",
                    userId, destination, message);
            return false;
        }

        try {
            // Check if user is connected
            if (userRegistry.getUser(userId) == null) {
                log.debug("User {} not connected, skipping message to {}", userId, destination);
                return false;
            }

            // Check failure count
            AtomicInteger failures = failureCount.get(userId);
            if (failures != null && failures.get() >= MAX_FAILURES) {
                log.warn("Too many failures for user {}, skipping message", userId);
                return false;
            }

            messagingTemplate.convertAndSendToUser(userId, destination, message);

            // Reset failure count on success
            failureCount.remove(userId);
            log.debug("Message sent to user {}: {}", userId, destination);
            return true;

        } catch (Exception e) {
            // Increment failure count
            failureCount.computeIfAbsent(userId, k -> new AtomicInteger(0)).incrementAndGet();

            log.warn("Failed to send message to user {} at {}: {}",
                    userId, destination, e.getMessage());
            return false;
        }
    }

    /**
     * Send message to a topic (broadcast) with error handling
     */
    public boolean sendToTopic(String destination, Object message) {
        if (destination == null || message == null) {
            log.warn("Invalid parameters for sendToTopic: destination={}, message={}",
                    destination, message);
            return false;
        }

        try {
            messagingTemplate.convertAndSend(destination, message);
            log.debug("Message sent to topic: {}", destination);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send message to topic {}: {}", destination, e.getMessage());
            return false;
        }
    }

    /**
     * Send message to a specific session with validation
     */
    public boolean sendToSession(String sessionId, String destination, Object message) {
        if (sessionId == null || destination == null || message == null) {
            log.warn("Invalid parameters for sendToSession: sessionId={}, destination={}, message={}",
                    sessionId, destination, message);
            return false;
        }

        try {
            // Check if session is still active
            if (!sessionManager.isSessionActive(sessionId)) {
                log.debug("Session {} not active, skipping message to {}", sessionId, destination);
                return false;
            }

            // Check failure count
            AtomicInteger failures = failureCount.get(sessionId);
            if (failures != null && failures.get() >= MAX_FAILURES) {
                log.warn("Too many failures for session {}, skipping message", sessionId);
                return false;
            }

            messagingTemplate.convertAndSendToUser(sessionId, destination, message,
                    createHeaders(sessionId));

            // Reset failure count on success
            failureCount.remove(sessionId);
            log.debug("Message sent to session {}: {}", sessionId, destination);
            return true;

        } catch (Exception e) {
            // Increment failure count
            failureCount.computeIfAbsent(sessionId, k -> new AtomicInteger(0)).incrementAndGet();

            log.warn("Failed to send message to session {} at {}: {}",
                    sessionId, destination, e.getMessage());
            return false;
        }
    }

    /**
     * Async method to send message to user (non-blocking)
     */
    @Async
    public CompletableFuture<Boolean> sendToUserAsync(String userId, String destination, Object message) {
        return CompletableFuture.completedFuture(sendToUser(userId, destination, message));
    }

    /**
     * Async method to send message to topic (non-blocking)
     */
    @Async
    public CompletableFuture<Boolean> sendToTopicAsync(String destination, Object message) {
        return CompletableFuture.completedFuture(sendToTopic(destination, message));
    }

    /**
     * Send message with retry mechanism
     */
    public boolean sendWithRetry(String userId, String destination, Object message, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (sendToUser(userId, destination, message)) {
                return true;
            }

            if (attempt < maxRetries) {
                try {
                    Thread.sleep(100 * attempt); // Exponential backoff
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return false;
    }

    /**
     * Broadcast message to all users in a game session
     */
    public void broadcastToGameSession(String gameSessionId, Object message) {
        String destination = "/topic/game/" + gameSessionId;
        sendToTopic(destination, message);
    }

    /**
     * Send game update to specific players
     */
    public void sendGameUpdate(String gameSessionId, String playerId, Object gameUpdate) {
        String destination = "/queue/game-updates";
        sendToUser(playerId, destination, gameUpdate);
    }

    /**
     * Send move validation result
     */
    public void sendMoveValidation(String playerId, Object validationResult) {
        String destination = "/queue/move-validation";
        sendToUser(playerId, destination, validationResult);
    }

    /**
     * Send error message to user
     */
    public void sendError(String userId, String errorMessage) {
        String destination = "/queue/errors";
        sendToUser(userId, destination, new ErrorMessage(errorMessage, System.currentTimeMillis()));
    }

    /**
     * Get active connection count
     */
    public int getActiveConnectionCount() {
        return sessionManager.getActiveSessionCount();
    }

    /**
     * Check if a user is connected
     */
    public boolean isUserConnected(String userId) {
        return userRegistry.getUser(userId) != null;
    }

    private java.util.Map<String, Object> createHeaders(String sessionId) {
        java.util.Map<String, Object> headers = new java.util.HashMap<>();
        headers.put("simpSessionId", sessionId);
        return headers;
    }

    /**
     * Cleanup failed sessions periodically
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void cleanupFailedSessions() {
        int cleanedUp = 0;
        var iterator = failureCount.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            String sessionId = entry.getKey();

            // Remove if session is no longer active or has too many failures
            if (!sessionManager.isSessionActive(sessionId) ||
                    entry.getValue().get() >= MAX_FAILURES) {
                iterator.remove();
                cleanedUp++;
            }
        }

        if (cleanedUp > 0) {
            log.info("Cleaned up {} failed session entries", cleanedUp);
        }

        // Also cleanup session manager
        sessionManager.cleanupClosedSessions();
    }

    /**
     * Error message wrapper class
     */
    public static class ErrorMessage {
        private final String message;
        private final long timestamp;

        public ErrorMessage(String message, long timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }

        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
}