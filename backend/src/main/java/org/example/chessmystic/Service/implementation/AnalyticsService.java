package org.example.chessmystic.Service.implementation;

import org.example.chessmystic.Models.Analytics.GameAnalytics;
import org.example.chessmystic.Models.Analytics.UserAnalytics;
import org.example.chessmystic.Models.KafkaEvents.*;
import org.example.chessmystic.Repository.AnalyticsRepository;
import org.example.chessmystic.Repository.UserAnalyticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);

    @Autowired
    private AnalyticsRepository gameAnalyticsRepository;

    @Autowired
    private UserAnalyticsRepository userAnalyticsRepository;

    // Temporary in-memory storage for active games (will be persisted on game end)
    private final Map<String, GameAnalytics> activeGames = new ConcurrentHashMap<>();

    @Transactional
    public void processMoveEvent(MoveEvent moveEvent) {
        try {
            String gameId = moveEvent.getGameId();
            GameAnalytics analytics = activeGames.computeIfAbsent(gameId, k -> 
                GameAnalytics.builder()
                    .gameId(gameId)
                    .totalMoves(0)
                    .whiteMoves(0)
                    .blackMoves(0)
                    .captureCount(0)
                    .checkCount(0)
                    .checkmate(false)
                    .createdAt(LocalDateTime.now())
                    .build()
            );

            analytics.setTotalMoves(analytics.getTotalMoves() + 1);
            
            if ("WHITE".equalsIgnoreCase(moveEvent.getPieceColor())) {
                analytics.setWhiteMoves(analytics.getWhiteMoves() + 1);
            } else {
                analytics.setBlackMoves(analytics.getBlackMoves() + 1);
            }

            if (moveEvent.isCapture()) {
                analytics.setCaptureCount(analytics.getCaptureCount() + 1);
            }

            if (moveEvent.isCheck()) {
                analytics.setCheckCount(analytics.getCheckCount() + 1);
            }

            if (moveEvent.isCheckmate()) {
                analytics.setCheckmate(true);
            }

            analytics.setUpdatedAt(LocalDateTime.now());
            activeGames.put(gameId, analytics);

            logger.debug("Move event processed for game: {}, total moves: {}", 
                gameId, analytics.getTotalMoves());

        } catch (Exception e) {
            logger.error("Error processing move event: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void processGameStartEvent(GameStartEvent event) {
        try {
            GameAnalytics analytics = GameAnalytics.builder()
                .gameId(event.getGameId())
                .whitePlayerId(event.getPlayerId())
                .blackPlayerId(event.getOpponentId())
                .gameMode(event.getGameMode())
                .gameStartTime(LocalDateTime.now())
                .detectedOpening(event.getOpeningDetected())
                .totalMoves(0)
                .whiteMoves(0)
                .blackMoves(0)
                .captureCount(0)
                .checkCount(0)
                .checkmate(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

            activeGames.put(event.getGameId(), analytics);

            logger.debug("Game start event processed: {}", event.getGameId());

        } catch (Exception e) {
            logger.error("Error processing game start event: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void processGameEndEvent(GameEndEvent event) {
        try {
            GameAnalytics analytics = activeGames.remove(event.getGameId());
            if (analytics != null) {
                analytics.setGameEndTime(LocalDateTime.now());
                analytics.setWinnerId(event.getWinnerId());
                analytics.setEndReason(event.getEndReason());
                analytics.setTotalMoves(event.getTotalMoves());
                
                if (analytics.getGameStartTime() != null) {
                    long durationMs = java.time.Duration.between(
                        analytics.getGameStartTime(), 
                        analytics.getGameEndTime()
                    ).toMillis();
                    analytics.setGameDurationMs(durationMs);
                }

                analytics.setUpdatedAt(LocalDateTime.now());
                
                // Persist to database
                gameAnalyticsRepository.save(analytics);
                
                // Update user analytics
                updateUserAnalytics(analytics);

                logger.info("Game ended and persisted: gameId={}, winner={}, moves={}, duration={}ms",
                    event.getGameId(), event.getWinnerId(), event.getTotalMoves(), 
                    analytics.getGameDurationMs());
            } else {
                logger.warn("Game end event received for unknown game: {}", event.getGameId());
            }

        } catch (Exception e) {
            logger.error("Error processing game end event: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void processUserAction(UserActionEvent event) {
        try {
            UserAnalytics userAnalytics = userAnalyticsRepository.findByUserId(event.getUserId())
                .orElse(UserAnalytics.builder()
                    .userId(event.getUserId())
                    .totalGamesPlayed(0)
                    .gamesWon(0)
                    .gamesLost(0)
                    .gamesDrawn(0)
                    .winRate(0.0)
                    .totalMovesPlayed(0)
                    .totalGameTimeMs(0L)
                    .actionCounts(new ConcurrentHashMap<>())
                    .createdAt(LocalDateTime.now())
                    .build());

            // Update action counts
            userAnalytics.getActionCounts().merge(event.getActionType(), 1, Integer::sum);
            userAnalytics.setLastActivityTime(LocalDateTime.now());
            userAnalytics.setUpdatedAt(LocalDateTime.now());

            userAnalyticsRepository.save(userAnalytics);

            logger.debug("User action processed: userId={}, action={}", 
                event.getUserId(), event.getActionType());

        } catch (Exception e) {
            logger.error("Error processing user action: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void processPerformanceMetrics(PerformanceMetricsEvent event) {
        try {
            // Log performance metrics and potentially alert if thresholds exceeded
            logger.debug("Performance metrics processed: gameId={}, cpu={}%, memory={}MB",
                event.getGameId(), event.getCpuUsage(), event.getMemoryUsage() / 1024 / 1024);

            // Alert if performance is poor
            if (event.getCpuUsage() > 80.0 || event.getMemoryUsage() > 1000000000L) {
                logger.warn("Poor performance detected: gameId={}, cpu={}%, memory={}MB",
                    event.getGameId(), event.getCpuUsage(), event.getMemoryUsage() / 1024 / 1024);
            }

        } catch (Exception e) {
            logger.error("Error processing performance metrics: {}", e.getMessage(), e);
        }
    }

    private void updateUserAnalytics(GameAnalytics gameAnalytics) {
        try {
            // Update white player analytics
            updatePlayerAnalytics(gameAnalytics.getWhitePlayerId(), gameAnalytics, 
                gameAnalytics.getWinnerId() != null && gameAnalytics.getWinnerId().equals(gameAnalytics.getWhitePlayerId()));

            // Update black player analytics
            if (gameAnalytics.getBlackPlayerId() != null) {
                updatePlayerAnalytics(gameAnalytics.getBlackPlayerId(), gameAnalytics,
                    gameAnalytics.getWinnerId() != null && gameAnalytics.getWinnerId().equals(gameAnalytics.getBlackPlayerId()));
            }

        } catch (Exception e) {
            logger.error("Error updating user analytics: {}", e.getMessage(), e);
        }
    }

    private void updatePlayerAnalytics(String playerId, GameAnalytics gameAnalytics, boolean won) {
        UserAnalytics userAnalytics = userAnalyticsRepository.findByUserId(playerId)
            .orElse(UserAnalytics.builder()
                .userId(playerId)
                .totalGamesPlayed(0)
                .gamesWon(0)
                .gamesLost(0)
                .gamesDrawn(0)
                .winRate(0.0)
                .totalMovesPlayed(0)
                .totalGameTimeMs(0L)
                .actionCounts(new ConcurrentHashMap<>())
                .createdAt(LocalDateTime.now())
                .build());

        userAnalytics.setTotalGamesPlayed(userAnalytics.getTotalGamesPlayed() + 1);
        
        if (won) {
            userAnalytics.setGamesWon(userAnalytics.getGamesWon() + 1);
            userAnalytics.setCurrentWinStreak(userAnalytics.getCurrentWinStreak() + 1);
            userAnalytics.setLongestWinStreak(Math.max(userAnalytics.getLongestWinStreak(), userAnalytics.getCurrentWinStreak()));
        } else if (gameAnalytics.getWinnerId() != null) {
            userAnalytics.setGamesLost(userAnalytics.getGamesLost() + 1);
            userAnalytics.setCurrentWinStreak(0);
        } else {
            userAnalytics.setGamesDrawn(userAnalytics.getGamesDrawn() + 1);
            userAnalytics.setCurrentWinStreak(0);
        }

        // Calculate win rate
        if (userAnalytics.getTotalGamesPlayed() > 0) {
            userAnalytics.setWinRate((double) userAnalytics.getGamesWon() / userAnalytics.getTotalGamesPlayed());
        }

        // Update other metrics
        userAnalytics.setTotalMovesPlayed(userAnalytics.getTotalMovesPlayed() + gameAnalytics.getTotalMoves());
        userAnalytics.setTotalGameTimeMs(userAnalytics.getTotalGameTimeMs() + gameAnalytics.getGameDurationMs());
        userAnalytics.setMostPlayedGameMode(gameAnalytics.getGameMode());
        userAnalytics.setLastActivityTime(LocalDateTime.now());
        userAnalytics.setUpdatedAt(LocalDateTime.now());

        userAnalyticsRepository.save(userAnalytics);
    }

    // Cleanup method for orphaned active games (call periodically)
    @Transactional
    public void cleanupOrphanedGames() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
            activeGames.entrySet().removeIf(entry -> 
                entry.getValue().getCreatedAt().isBefore(cutoff));
            
            logger.info("Cleaned up orphaned active games, remaining: {}", activeGames.size());
        } catch (Exception e) {
            logger.error("Error cleaning up orphaned games: {}", e.getMessage(), e);
        }
    }
}
