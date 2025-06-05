package org.example.chessmystic.Service.interfaces.GameRelated;

import org.example.chessmystic.Models.Tracking.GameResult;
import org.example.chessmystic.Models.Tracking.GameHistory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IGameHistoryService {
    GameHistory createGameHistory(String gameSessionId, List<String> userIds, boolean isRPGMode);
    GameHistory completeGameHistory(String gameHistoryId, GameResult result);
    Optional<GameHistory> findById(String id);
    List<GameHistory> findByUserId(String userId);
    List<GameHistory> findByGameSessionId(String gameSessionId);
    List<GameHistory> findRankedGamesByUser(String userId);
    List<GameHistory> findRPGGamesByUser(String userId);
    List<GameHistory> findRecentGames(LocalDateTime since);
    GameHistory addPlayerAction(String gameHistoryId, String playerActionId);
}
