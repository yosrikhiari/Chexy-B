package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;

@Service
public class MatchmakingService {

    private List<MatchmakingPlayer> queue = new ArrayList<>();
    private final GameSessionService gameSessionService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public MatchmakingService(GameSessionService gameSessionService, SimpMessagingTemplate messagingTemplate) {
        this.gameSessionService = gameSessionService;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRate = 5000)
    public void matchPlayers() {
        List<MatchmakingPlayer> sortedQueue = new ArrayList<>(queue);
        sortedQueue.sort(Comparator.comparing(p -> p.joinTime));

        while (sortedQueue.size() >= 2) {
            MatchmakingPlayer player1 = sortedQueue.get(0);
            MatchmakingPlayer player2 = findMatch(player1, sortedQueue.subList(1, sortedQueue.size()));

            if (player2 != null) {
                createGameSession(player1, player2);
                queue.remove(player1);
                queue.remove(player2);
                sortedQueue.remove(player1);
                sortedQueue.remove(player2);
            } else {
                break;
            }
        }
    }

    public void joinQueue(String userId, int points) {
        queue.add(new MatchmakingPlayer(userId, points, LocalDateTime.now()));
        messagingTemplate.convertAndSend("/topic/matchmaking/status", queue.size());
    }

    public void leaveQueue(String userId) {
        queue.removeIf(p -> p.userId.equals(userId));
        messagingTemplate.convertAndSend("/topic/matchmaking/status", queue.size());
    }

    private MatchmakingPlayer findMatch(MatchmakingPlayer player, List<MatchmakingPlayer> candidates) {
        LocalDateTime now = LocalDateTime.now();
        boolean waitingTooLong = Duration.between(player.joinTime, now).toSeconds() > 90;

        if (waitingTooLong) {
            return candidates.stream()
                    .min(Comparator.comparing(p -> p.joinTime))
                    .orElse(null);
        } else {
            return candidates.stream()
                    .filter(p -> Math.abs(p.points - player.points) <= 100)
                    .min(Comparator.comparingInt(p -> Math.abs(p.points - player.points)))
                    .orElse(null);
        }
    }

    private void createGameSession(MatchmakingPlayer player1, MatchmakingPlayer player2) {
        if (new Random().nextBoolean()) {
            MatchmakingPlayer temp = player1;
            player1 = player2;
            player2 = temp;
        }
        GameSession session = gameSessionService.createGameSession(player1.userId, GameMode.CLASSIC_MULTIPLAYER, false, "aaa");
        session = gameSessionService.joinGame(session.getGameId(), player2.userId, null);
        session = gameSessionService.startGame(session.getGameId());
        messagingTemplate.convertAndSendToUser(player1.userId, "/queue/matchmaking/match",
                Map.of("gameId", session.getGameId(), "opponentId", player2.userId, "color", "white"));
        messagingTemplate.convertAndSendToUser(player2.userId, "/queue/matchmaking/match",
                Map.of("gameId", session.getGameId(), "opponentId", player1.userId, "color", "black"));
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
}