package org.example.chessmystic.Service.interfaces.GameRelated;

import org.example.chessmystic.Models.Tracking.GameResult;
import org.example.chessmystic.Models.Tracking.GameHistory;
import org.example.chessmystic.Models.Tracking.GameSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IGameHistoryService {
    GameHistory createGameHistory(GameSession session);
    GameHistory updateGameHistory(String gameHistoryId, GameResult result, LocalDateTime endTime);
    GameHistory addPlayerAction(String gameHistoryId, String playerActionId);
    Optional<GameHistory> findById(String id);
    List<GameHistory> findByUserId(String userId);
    Optional<GameHistory> findByGameSessionId(String gameSessionId);
    List<GameHistory> findRankedGamesByUser(String userId);
    List<GameHistory> findRPGGamesByUser(String userId);
    List<GameHistory> findRecentGames(LocalDateTime since);
}