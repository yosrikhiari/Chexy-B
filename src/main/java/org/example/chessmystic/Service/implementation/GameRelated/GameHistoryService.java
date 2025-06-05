package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.Tracking.GameResult;
import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Models.Tracking.GameHistory;
import org.example.chessmystic.Repository.GameHistoryRepository;
import org.example.chessmystic.Service.implementation.UserService;
import org.example.chessmystic.Service.interfaces.GameRelated.IGameHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GameHistoryService implements IGameHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(GameHistoryService.class);
    private final GameHistoryRepository gameHistoryRepository;
    private final GameSessionService gameSessionService;
    private final UserService userService;

    @Autowired
    public GameHistoryService(GameHistoryRepository gameHistoryRepository, GameSessionService gameSessionService, UserService userService) {
        this.gameHistoryRepository = gameHistoryRepository;
        this.gameSessionService = gameSessionService;
        this.userService = userService;
    }

    @Override
    @Transactional
    public GameHistory createGameHistory(String gameSessionId, List<String> userIds, boolean isRPGMode) {
        logger.info("Creating game history for session: {}", gameSessionId);
        gameSessionService.findById(gameSessionId)
                .orElseThrow(() -> new RuntimeException("Game session not found"));

        for (String userId : userIds) {
            userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        }

        GameHistory gameHistory = GameHistory.builder()
                .gameSessionId(gameSessionId)
                .userIds(userIds)
                .startTime(LocalDateTime.now())
                .isRanked(false)
                .isRPGMode(isRPGMode)
                .gameMode(isRPGMode ? GameMode.SINGLE_PLAYER_RPG.name() : GameMode.CLASSIC_MULTIPLAYER.name())
                .totalMoves(0)
                .playerActionIds(new ArrayList<>())
                .build();

        GameHistory savedHistory = gameHistoryRepository.save(gameHistory);
        logger.info("Game history created: {}", savedHistory.getId());
        return savedHistory;
    }

    @Override
    @Transactional
    public GameHistory completeGameHistory(String gameHistoryId, GameResult result) {
        GameHistory gameHistory = gameHistoryRepository.findById(gameHistoryId)
                .orElseThrow(() -> new RuntimeException("Game history not found"));

        gameHistory.setResult(result);
        gameHistory.setEndTime(LocalDateTime.now());
        gameHistory.setFinalScore(result.getPointsAwarded());
        if (gameHistory.isRPGMode()) {
            // Assuming round tracking is managed elsewhere (e.g., RPGGameService)
            gameHistory.setFinalRound(gameHistory.getFinalRound());
        }

        GameHistory updatedHistory = gameHistoryRepository.save(gameHistory);
        logger.info("Game history completed: {}", gameHistoryId);
        return updatedHistory;
    }

    @Override
    public Optional<GameHistory> findById(String id) {
        return gameHistoryRepository.findById(id);
    }

    @Override
    public List<GameHistory> findByUserId(String userId) {
        return gameHistoryRepository.findByUserIdsContaining(userId);
    }

    @Override
    public List<GameHistory> findByGameSessionId(String gameSessionId) {
        return gameHistoryRepository.findByGameSessionId(gameSessionId);
    }

    @Override
    public List<GameHistory> findRankedGamesByUser(String userId) {
        return gameHistoryRepository.findRankedGamesByUserId(userId);
    }

    @Override
    public List<GameHistory> findRPGGamesByUser(String userId) {
        return gameHistoryRepository.findByUserIdsContaining(userId).stream()
                .filter(GameHistory::isRPGMode)
                .toList();
    }

    @Override
    public List<GameHistory> findRecentGames(LocalDateTime since) {
        return gameHistoryRepository.findByStartTimeBetween(since, LocalDateTime.now());
    }

    @Override
    @Transactional
    public GameHistory addPlayerAction(String gameHistoryId, String playerActionId) {
        GameHistory gameHistory = gameHistoryRepository.findById(gameHistoryId)
                .orElseThrow(() -> new RuntimeException("Game history not found"));

        if (gameHistory.getPlayerActionIds() == null) {
            gameHistory.setPlayerActionIds(new ArrayList<>());
        }
        gameHistory.getPlayerActionIds().add(playerActionId);
        gameHistory.setTotalMoves(gameHistory.getTotalMoves() + 1);

        GameHistory updatedHistory = gameHistoryRepository.save(gameHistory);
        logger.info("Player action added to game history: {}", gameHistoryId);
        return updatedHistory;
    }
}