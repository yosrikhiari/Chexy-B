package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Models.Tracking.GameHistory;
import org.example.chessmystic.Models.Tracking.GameResult;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Repository.GameHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
public class GameHistoryService {

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
    public void updateGameHistory(String gameHistoryId, GameResult result, LocalDateTime endTime) {
        GameHistory history = gameHistoryRepository.findById(gameHistoryId)
                .orElseThrow(() -> new RuntimeException("Game history not found"));
        history.setEndTime(endTime);
        history.setResult(result);
        history.setTotalMoves(history.getPlayerActionIds().size());
        gameHistoryRepository.save(history);
    }

    @Transactional
    public void addPlayerAction(String gameHistoryId, String playerActionId) {
        GameHistory history = gameHistoryRepository.findById(gameHistoryId)
                .orElseThrow(() -> new RuntimeException("Game history not found"));
        history.getPlayerActionIds().add(playerActionId);
        gameHistoryRepository.save(history);
    }
}