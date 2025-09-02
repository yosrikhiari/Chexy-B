package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MatchmakingService {

    private static final Logger logger = LoggerFactory.getLogger(MatchmakingService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final GameSessionService gameSessionService;
    private final List<MatchmakingPlayer> queue = new ArrayList<>();
    private final Object queueLock = new Object();
    private final Map<String, PendingMatch> pendingMatches = new ConcurrentHashMap<>();
    private final Map<String, MatchmakingPlayer> playersInQueue = new ConcurrentHashMap<>();

    @Autowired
    public MatchmakingService(GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    public void joinQueue(String userId, int points) {
        synchronized (queueLock) {
            if (playersInQueue.containsKey(userId)) {
                logger.warn("User {} already in queue", userId);
                messagingTemplate.convertAndSendToUser(userId, "/queue/matchmaking/error",
                        Map.of("message", "You are already in the matchmaking queue"));
                return;
            }

            MatchmakingPlayer player = new MatchmakingPlayer(userId, points, LocalDateTime.now());
            queue.add(player);
            playersInQueue.put(userId, player);
            logger.info("Player {} joined queue with {} points. Queue size: {}", userId, points, queue.size());
            messagingTemplate.convertAndSend("/topic/matchmaking/status",
                    Map.of("playersInQueue", queue.size()));
        }
    }

    public int getQueueSize() {
        synchronized (queueLock) {
            return queue.size();
        }
    }

    public void leaveQueue(String userId) {
        synchronized (queueLock) {
            boolean removed = queue.removeIf(p -> p.userId.equals(userId));
            if (removed) {
                playersInQueue.remove(userId);
                logger.info("Player {} left queue. Queue size: {}", userId, queue.size());
                messagingTemplate.convertAndSend("/topic/matchmaking/status",
                        Map.of("playersInQueue", queue.size()));
            }
        }
    }

    @Scheduled(fixedRate = 5000) // Run every 5 seconds
    public void matchPlayers() {
        synchronized (queueLock) {
            if (queue.size() < 2) {
                return;
            }

            List<MatchmakingPlayer> sortedQueue = new ArrayList<>(queue);
            sortedQueue.sort(Comparator.comparing(p -> p.joinTime));

            Iterator<MatchmakingPlayer> iterator = sortedQueue.iterator();
            while (iterator.hasNext() && sortedQueue.size() >= 2) {
                MatchmakingPlayer player1 = iterator.next();

                // Find candidates excluding the current player
                List<MatchmakingPlayer> candidates = sortedQueue.stream()
                        .filter(p -> !p.userId.equals(player1.userId))
                        .toList();

                MatchmakingPlayer player2 = findMatch(player1, candidates);

                if (player2 != null) {
                    logger.info("Matched {} ({} points) with {} ({} points)",
                            player1.userId, player1.points, player2.userId, player2.points);

                    createPendingMatch(player1, player2);

                    // Remove matched players from queue
                    queue.remove(player1);
                    queue.remove(player2);
                    sortedQueue.remove(player1);
                    sortedQueue.remove(player2);

                    // Update queue status
                    messagingTemplate.convertAndSend("/topic/matchmaking/status",
                            Map.of("playersInQueue", queue.size()));
                }
            }
        }
    }

    private MatchmakingPlayer findMatch(MatchmakingPlayer player, List<MatchmakingPlayer> candidates) {
        LocalDateTime now = LocalDateTime.now();
        long waitSeconds = Duration.between(player.joinTime, now).toSeconds();

        // First try to find a close match (within 50 points)
        Optional<MatchmakingPlayer> closeMatch = candidates.stream()
                .filter(p -> Math.abs(p.points - player.points) <= 50)
                .min(Comparator.comparingInt(p -> Math.abs(p.points - player.points)));

        if (closeMatch.isPresent()) {
            return closeMatch.get();
        }

        // If waited more than 2 minutes, match with anyone
        if (waitSeconds > 120) {
            return candidates.stream()
                    .min(Comparator.comparingInt(p -> Math.abs(p.points - player.points)))
                    .orElse(null);
        }

        return null;
    }

    private void createPendingMatch(MatchmakingPlayer player1, MatchmakingPlayer player2) {
        String matchId = UUID.randomUUID().toString();
        PendingMatch pendingMatch = new PendingMatch(matchId, player1, player2);
        pendingMatches.put(matchId, pendingMatch);

        // Send match found notifications to both players
        Map<String, Object> matchData1 = Map.of(
                "matchId", matchId,
                "opponentId", player2.userId,
                "opponentPoints", player2.points
        );
        messagingTemplate.convertAndSend("/queue/matchmaking.matchFound." + player1.userId, matchData1);

        Map<String, Object> matchData2 = Map.of(
                "matchId", matchId,
                "opponentId", player1.userId,
                "opponentPoints", player1.points
        );
        messagingTemplate.convertAndSend("/queue/matchmaking.matchFound." + player2.userId, matchData2);

        // Set up timeout timer
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                PendingMatch match = pendingMatches.get(matchId);
                if (match != null && (!match.player1Accepted || !match.player2Accepted)) {
                    cancelPendingMatch(match, "Match acceptance timed out");
                }
            }
        }, 30000); // 30 seconds timeout

        playersInQueue.remove(player1.userId);
        playersInQueue.remove(player2.userId);
        pendingMatch.timeoutTimer = timer;
        pendingMatches.put(matchId, pendingMatch);
    }

    public void acceptMatch(String matchId, String userId) {
        PendingMatch pendingMatch = pendingMatches.get(matchId);
        if (pendingMatch == null) {
            logger.warn("Accept match called for non-existent match: {}", matchId);
            return;
        }

        if (pendingMatch.player1.userId.equals(userId)) {
            pendingMatch.player1Accepted = true;
            logger.info("Player {} accepted match {}", userId, matchId);
        } else if (pendingMatch.player2.userId.equals(userId)) {
            pendingMatch.player2Accepted = true;
            logger.info("Player {} accepted match {}", userId, matchId);
        } else {
            logger.warn("User {} not part of match {}", userId, matchId);
            return;
        }

        checkMatchAcceptance(pendingMatch);
    }

    public void declineMatch(String matchId, String userId) {
        PendingMatch pendingMatch = pendingMatches.get(matchId);
        if (pendingMatch != null) {
            logger.info("Player {} declined match {}", userId, matchId);

            // Cancel the match but don't re-add the declining player to queue
            cancelPendingMatchWithDecliningUser(pendingMatch, userId, "Match declined by " + userId);
        }
    }

    private void cancelPendingMatchWithDecliningUser(PendingMatch pendingMatch, String decliningUserId, String reason) {
        logger.info("Cancelling match {}: {}", pendingMatch.matchId, reason);

        // Determine which player is declining and which is the opponent
        String opponentId = pendingMatch.player1.userId.equals(decliningUserId)
                ? pendingMatch.player2.userId
                : pendingMatch.player1.userId;

        // Send different messages to declining user vs opponent
        Map<String, Object> decliningUserData = Map.of(
                "message", "You declined the match and left the queue",
                "leftQueue", true
        );
        Map<String, Object> opponentData = Map.of(
                "message", "Opponent declined the match. You remain in queue",
                "leftQueue", false
        );

        messagingTemplate.convertAndSend("/queue/matchmaking.matchCancelled." + decliningUserId, decliningUserData);
        messagingTemplate.convertAndSend("/queue/matchmaking.matchCancelled." + opponentId, opponentData);

        // Re-add only the non-declining player to queue
        synchronized (queueLock) {
            if (!pendingMatch.player1.userId.equals(decliningUserId)) {
                queue.add(pendingMatch.player1);
                logger.info("Re-added player {} to queue after opponent declined", pendingMatch.player1.userId);
            } else {
                logger.info("Player {} left queue after declining match", pendingMatch.player1.userId);
            }

            if (!pendingMatch.player2.userId.equals(decliningUserId)) {
                queue.add(pendingMatch.player2);
                logger.info("Re-added player {} to queue after opponent declined", pendingMatch.player2.userId);
            } else {
                logger.info("Player {} left queue after declining match", pendingMatch.player2.userId);
            }

            // Update queue status for everyone
            messagingTemplate.convertAndSend("/topic/matchmaking/status",
                    Map.of("playersInQueue", queue.size()));
        }

        // Clean up
        pendingMatch.timeoutTimer.cancel();
        pendingMatches.remove(pendingMatch.matchId);
    }


    private void checkMatchAcceptance(PendingMatch pendingMatch) {
        if (pendingMatch.player1Accepted && pendingMatch.player2Accepted) {
            logger.info("Both players accepted match {}", pendingMatch.matchId);
            createGameSession(pendingMatch.player1, pendingMatch.player2);

            // Clean up
            pendingMatch.timeoutTimer.cancel();
            pendingMatches.remove(pendingMatch.matchId);
        }
    }

    private void cancelPendingMatch(PendingMatch pendingMatch, String reason) {
        logger.info("Cancelling match {}: {}", pendingMatch.matchId, reason);

        // Notify both players
        Map<String, Object> cancellationData = Map.of("message", reason);
        messagingTemplate.convertAndSend("/queue/matchmaking.matchCancelled." + pendingMatch.player1.userId, cancellationData);
        messagingTemplate.convertAndSend("/queue/matchmaking.matchCancelled." + pendingMatch.player2.userId, cancellationData);

        // Re-add players to queue
        synchronized (queueLock) {
            queue.add(pendingMatch.player1);
            queue.add(pendingMatch.player2);
            messagingTemplate.convertAndSend("/exchange/amq.topic/matchmaking.status",
                    Map.of("playersInQueue", queue.size()));
        }

        // Clean up
        pendingMatch.timeoutTimer.cancel();
        pendingMatches.remove(pendingMatch.matchId);
    }

    private void createGameSession(MatchmakingPlayer player1, MatchmakingPlayer player2) {
        try {
            // Randomly assign colors
            boolean player1White = new Random().nextBoolean();
            MatchmakingPlayer whitePlayer = player1White ? player1 : player2;
            MatchmakingPlayer blackPlayer = player1White ? player2 : player1;

            // Create game session
            GameSession session = gameSessionService.createGameSession(
                    whitePlayer.userId, GameMode.CLASSIC_MULTIPLAYER, false, null);
            session = gameSessionService.joinGame(session.getGameId(), blackPlayer.userId, null);
            session = gameSessionService.startGame(session.getGameId());

            // Notify both players with game details
            Map<String, Object> gameData1 = Map.of(
                    "gameId", session.getGameId(),
                    "opponentId", blackPlayer.userId,
                    "color", "white"
            );
            messagingTemplate.convertAndSend("/queue/matchmaking.gameReady." + whitePlayer.userId, gameData1);

            Map<String, Object> gameData2 = Map.of(
                    "gameId", session.getGameId(),
                    "opponentId", whitePlayer.userId,
                    "color", "black"
            );
            messagingTemplate.convertAndSend("/queue/matchmaking.gameReady." + blackPlayer.userId, gameData2);

            logger.info("Game session {} created for {} (white) vs {} (black)",
                    session.getGameId(), whitePlayer.userId, blackPlayer.userId);
        } catch (Exception e) {
            logger.error("Failed to create game session for {} vs {}: {}",
                    player1.userId, player2.userId, e.getMessage());

            // Notify players of error
            Map<String, Object> errorData = Map.of("message", "Failed to create game session");
            messagingTemplate.convertAndSend("/queue/matchmaking.error." + player1.userId, errorData);
            messagingTemplate.convertAndSend("/queue/matchmaking.error." + player2.userId, errorData);
        }
    }

    private static class MatchmakingPlayer {
        String userId;
        int points;
        LocalDateTime joinTime;

        MatchmakingPlayer(String userId, int points, LocalDateTime joinTime) {
            this.userId = userId;
            this.points = points;
            this.joinTime = joinTime;
        }
    }

    private static class PendingMatch {
        String matchId;
        MatchmakingPlayer player1;
        MatchmakingPlayer player2;
        boolean player1Accepted = false;
        boolean player2Accepted = false;
        LocalDateTime createdAt;
        Timer timeoutTimer;

        PendingMatch(String matchId, MatchmakingPlayer player1, MatchmakingPlayer player2) {
            this.matchId = matchId;
            this.player1 = player1;
            this.player2 = player2;
            this.createdAt = LocalDateTime.now();
        }
    }
}