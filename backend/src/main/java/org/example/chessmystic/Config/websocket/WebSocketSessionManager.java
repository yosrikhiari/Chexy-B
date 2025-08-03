package org.example.chessmystic.Config.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class WebSocketSessionManager {
    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionManager.class);

    private final ConcurrentMap<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesFailedToSend = new AtomicLong(0);

    /**
     * Session information wrapper
     */
    private static class SessionInfo {
        private final WebSocketSession session;
        private final long connectedTime;
        private volatile long lastActivity;
        private volatile int failureCount;
        private volatile boolean isHealthy;

        public SessionInfo(WebSocketSession session) {
            this.session = session;
            this.connectedTime = System.currentTimeMillis();
            this.lastActivity = this.connectedTime;
            this.failureCount = 0;
            this.isHealthy = true;
        }

        public WebSocketSession getSession() { return session; }
        public long getConnectedTime() { return connectedTime; }
        public long getLastActivity() { return lastActivity; }
        public void updateLastActivity() { this.lastActivity = System.currentTimeMillis(); }
        public int getFailureCount() { return failureCount; }
        public void incrementFailureCount() { this.failureCount++; }
        public void resetFailureCount() { this.failureCount = 0; }
        public boolean isHealthy() { return isHealthy && session.isOpen(); }
        public void markUnhealthy() { this.isHealthy = false; }

        public boolean isStale(long maxIdleTime) {
            return System.currentTimeMillis() - lastActivity > maxIdleTime;
        }
    }

    public void addSession(WebSocketSession session) {
        if (session != null && session.isOpen()) {
            SessionInfo sessionInfo = new SessionInfo(session);
            activeSessions.put(session.getId(), sessionInfo);
            log.info("Session added: {} (Total active: {})", session.getId(), activeSessions.size());
        }
    }

    public void removeSession(WebSocketSession session) {
        if (session != null) {
            SessionInfo removed = activeSessions.remove(session.getId());
            if (removed != null) {
                log.info("Session removed: {} (Total active: {})", session.getId(), activeSessions.size());

                // Gracefully close if still open
                if (session.isOpen()) {
                    try {
                        session.close(CloseStatus.NORMAL);
                    } catch (IOException e) {
                        log.debug("Error closing session {}: {}", session.getId(), e.getMessage());
                    }
                }
            }
        }
    }

    public void removeSessionById(String sessionId) {
        if (sessionId != null) {
            SessionInfo sessionInfo = activeSessions.remove(sessionId);
            if (sessionInfo != null) {
                log.info("Session removed by ID: {} (Total active: {})", sessionId, activeSessions.size());

                WebSocketSession session = sessionInfo.getSession();
                if (session != null && session.isOpen()) {
                    try {
                        session.close(CloseStatus.NORMAL);
                    } catch (IOException e) {
                        log.debug("Error closing session {}: {}", sessionId, e.getMessage());
                    }
                }
            }
        }
    }

    public WebSocketSession getSession(String sessionId) {
        SessionInfo sessionInfo = activeSessions.get(sessionId);
        if (sessionInfo != null && sessionInfo.isHealthy()) {
            sessionInfo.updateLastActivity();
            return sessionInfo.getSession();
        }
        return null;
    }

    public boolean isSessionActive(String sessionId) {
        SessionInfo sessionInfo = activeSessions.get(sessionId);
        if (sessionInfo == null) {
            return false;
        }

        boolean isActive = sessionInfo.isHealthy();
        if (isActive) {
            sessionInfo.updateLastActivity();
        }

        return isActive;
    }

    public Set<String> getActiveSessionIds() {
        return activeSessions.entrySet().stream()
                .filter(entry -> entry.getValue().isHealthy())
                .map(entry -> entry.getKey())
                .collect(Collectors.toSet());
    }

    public int getActiveSessionCount() {
        return (int) activeSessions.values().stream()
                .filter(SessionInfo::isHealthy)
                .count();
    }

    public void broadcastToActiveSessions(String message) {
        if (message == null) {
            log.warn("Cannot broadcast null message");
            return;
        }

        TextMessage textMessage = new TextMessage(message);
        int successCount = 0;
        int failureCount = 0;

        // Create a list of sessions to remove
        var sessionsToRemove = new java.util.ArrayList<String>();

        for (var entry : activeSessions.entrySet()) {
            String sessionId = entry.getKey();
            SessionInfo sessionInfo = entry.getValue();
            WebSocketSession session = sessionInfo.getSession();

            if (!session.isOpen()) {
                log.debug("Marking closed session for removal: {}", sessionId);
                sessionsToRemove.add(sessionId);
                continue;
            }

            try {
                session.sendMessage(textMessage);
                sessionInfo.updateLastActivity();
                sessionInfo.resetFailureCount();
                successCount++;
                messagesSent.incrementAndGet();

            } catch (Exception e) {
                log.warn("Failed to send message to session {}: {}", sessionId, e.getMessage());
                sessionInfo.incrementFailureCount();

                // Mark session as unhealthy if too many failures
                if (sessionInfo.getFailureCount() >= 3) {
                    sessionInfo.markUnhealthy();
                    sessionsToRemove.add(sessionId);
                }

                failureCount++;
                messagesFailedToSend.incrementAndGet();
            }
        }

        // Remove failed/closed sessions
        sessionsToRemove.forEach(this::removeSessionById);

        log.debug("Broadcast completed: {} successful, {} failed, {} sessions removed",
                successCount, failureCount, sessionsToRemove.size());
    }

    public boolean sendToSession(String sessionId, String message) {
        if (sessionId == null || message == null) {
            log.warn("Cannot send message: sessionId={}, message={}", sessionId, message != null);
            return false;
        }

        SessionInfo sessionInfo = activeSessions.get(sessionId);
        if (sessionInfo == null) {
            log.debug("Session not found: {}", sessionId);
            return false;
        }

        WebSocketSession session = sessionInfo.getSession();
        if (!session.isOpen()) {
            log.debug("Session closed, removing: {}", sessionId);
            activeSessions.remove(sessionId);
            return false;
        }

        try {
            session.sendMessage(new TextMessage(message));
            sessionInfo.updateLastActivity();
            sessionInfo.resetFailureCount();
            messagesSent.incrementAndGet();
            return true;

        } catch (Exception e) {
            log.warn("Failed to send message to session {}: {}", sessionId, e.getMessage());
            sessionInfo.incrementFailureCount();
            messagesFailedToSend.incrementAndGet();

            // Mark session as unhealthy if too many failures
            if (sessionInfo.getFailureCount() >= 3) {
                sessionInfo.markUnhealthy();
                removeSessionById(sessionId);
            }

            return false;
        }
    }

    /**
     * Get session statistics
     */
    public SessionStats getSessionStats() {
        long totalSessions = activeSessions.size();
        long healthySessions = activeSessions.values().stream()
                .filter(SessionInfo::isHealthy)
                .count();

        return new SessionStats(totalSessions, healthySessions,
                messagesSent.get(), messagesFailedToSend.get());
    }

    /**
     * Clean up closed and stale sessions - runs every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void cleanupClosedSessions() {
        long maxIdleTime = 5 * 60 * 1000; // 5 minutes
        int beforeCount = activeSessions.size();

        var sessionsToRemove = new java.util.ArrayList<String>();

        for (var entry : activeSessions.entrySet()) {
            String sessionId = entry.getKey();
            SessionInfo sessionInfo = entry.getValue();

            if (!sessionInfo.isHealthy() ||
                    sessionInfo.isStale(maxIdleTime) ||
                    sessionInfo.getFailureCount() >= 3) {
                sessionsToRemove.add(sessionId);
            }
        }

        sessionsToRemove.forEach(this::removeSessionById);

        int cleanedUp = beforeCount - activeSessions.size();
        if (cleanedUp > 0) {
            log.info("Cleaned up {} sessions (Active: {}, Healthy: {})",
                    cleanedUp, activeSessions.size(), getActiveSessionCount());
        }
    }

    /**
     * Force cleanup of all sessions (for shutdown)
     */
    public void closeAllSessions() {
        log.info("Closing all {} active sessions", activeSessions.size());

        for (SessionInfo sessionInfo : activeSessions.values()) {
            WebSocketSession session = sessionInfo.getSession();
            if (session.isOpen()) {
                try {
                    session.close(CloseStatus.GOING_AWAY);
                } catch (IOException e) {
                    log.debug("Error closing session during shutdown: {}", e.getMessage());
                }
            }
        }

        activeSessions.clear();
        log.info("All sessions closed");
    }

    /**
     * Session statistics class
     */
    public static class SessionStats {
        private final long totalSessions;
        private final long healthySessions;
        private final long messagesSent;
        private final long messagesFailedToSend;

        public SessionStats(long totalSessions, long healthySessions,
                            long messagesSent, long messagesFailedToSend) {
            this.totalSessions = totalSessions;
            this.healthySessions = healthySessions;
            this.messagesSent = messagesSent;
            this.messagesFailedToSend = messagesFailedToSend;
        }

        public long getTotalSessions() { return totalSessions; }
        public long getHealthySessions() { return healthySessions; }
        public long getMessagesSent() { return messagesSent; }
        public long getMessagesFailedToSend() { return messagesFailedToSend; }

        @Override
        public String toString() {
            return String.format("SessionStats{total=%d, healthy=%d, sent=%d, failed=%d}",
                    totalSessions, healthySessions, messagesSent, messagesFailedToSend);
        }
    }
}