package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Models.Tracking.GameHistory;
import org.example.chessmystic.Models.Tracking.GameResult;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Repository.GameHistoryRepository;
import org.example.chessmystic.Service.interfaces.GameRelated.IGameHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GameHistoryService implements IGameHistoryService {

    private final GameHistoryRepository gameHistoryRepository;

    @Autowired
    public GameHistoryService(GameHistoryRepository gameHistoryRepository) {
        this.gameHistoryRepository = gameHistoryRepository;
    }

    @Transactional
    public GameHistory createGameHistory(GameSession session) {
        GameHistory history = GameHistory.builder()
                .id(UUID.randomUUID().toString())
                .gameSessionId(session.getGameId())
                .userIds(session.getPlayerIds())
                .startTime(session.getStartedAt())
                .isRanked(session.isRankedMatch())
                .isRPGMode(session.getGameMode() == GameMode.SINGLE_PLAYER_RPG || session.getGameMode() == GameMode.MULTIPLAYER_RPG)
                .playerActionIds(new ArrayList<>())
                .build();
        return gameHistoryRepository.save(history);
    }

    @Transactional
    public GameHistory updateGameHistory(String gameHistoryId, GameResult result, LocalDateTime endTime) {
        GameHistory history = gameHistoryRepository.findById(gameHistoryId)
                .orElseThrow(() -> new RuntimeException("Game history not found"));
        history.setEndTime(endTime);
        history.setResult(result);
        history.setTotalMoves(history.getPlayerActionIds().size());
        gameHistoryRepository.save(history);
        return history;
    }

    @Transactional
    public GameHistory addPlayerAction(String gameHistoryId, String playerActionId) {
        GameHistory history = gameHistoryRepository.findById(gameHistoryId)
                .orElseThrow(() -> new RuntimeException("Game history not found"));
        history.getPlayerActionIds().add(playerActionId);
        gameHistoryRepository.save(history);
        return history;
    }

    @Transactional(readOnly = true)
    public Optional<GameHistory> findById(String id) {
        return gameHistoryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<GameHistory> findByUserId(String userId) {
        return gameHistoryRepository.findByUserIdsContaining(userId);
    }

    @Transactional(readOnly = true)
    public Optional<GameHistory> findByGameSessionId(String gameSessionId) {
        return gameHistoryRepository.findByGameSessionId(gameSessionId);
    }

    @Transactional(readOnly = true)
    public List<GameHistory> findRankedGamesByUser(String userId) {
        return gameHistoryRepository.findByUserIdsContainingAndIsRankedTrue(userId);
    }

    @Transactional(readOnly = true)
    public List<GameHistory> findRPGGamesByUser(String userId) {
        return gameHistoryRepository.findByUserIdsContainingAndIsRPGModeTrue(userId);
    }

    @Transactional(readOnly = true)
    public List<GameHistory> findRecentGames(LocalDateTime since) {
        return gameHistoryRepository.findByStartTimeAfter(since);
    }
}