package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Models.Mechanics.RPGGameState;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Models.Transactions.RPGModifier;
import org.example.chessmystic.Repository.GameSessionRepository;
import org.example.chessmystic.Repository.RPGGameStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Dedicated service for managing gold accumulation and persistence
 * Handles card-based bonuses and round rewards
 */
@Service
public class GoldManagementBackendService {
    private static final Logger logger = LoggerFactory.getLogger(GoldManagementBackendService.class);

    private final RPGGameStateRepository rpgGameStateRepository;
    private final GameSessionRepository gameSessionRepository;

    public GoldManagementBackendService(
            RPGGameStateRepository rpgGameStateRepository,
            GameSessionRepository gameSessionRepository) {
        this.rpgGameStateRepository = rpgGameStateRepository;
        this.gameSessionRepository = gameSessionRepository;
    }

    /**
     * Calculate gold multiplier bonus from active cards
     * Each "Gold Multiplier" card grants +10% bonus
     */
    public int calculateCardBonus(RPGGameState gameState) {
        if (gameState.getActiveModifiers() == null) {
            return 0;
        }

        long goldMultiplierCards = gameState.getActiveModifiers().stream()
                .filter(RPGModifier::isActive)
                .filter(mod -> {
                    String effect = mod.getEffect() != null ? mod.getEffect().toLowerCase() : "";
                    String name = mod.getName() != null ? mod.getName().toLowerCase() : "";
                    return effect.contains("gold_multiplier") ||
                            effect.contains("gold multiplier") ||
                            name.contains("gold multiplier");
                })
                .count();

        return (int) (goldMultiplierCards * 10); // 10% per card
    }

    /**
     * Calculate total round reward with bonuses
     * Base: 100 + (round * 10)
     * Then apply card bonus percentage
     */
    public int calculateRoundReward(int currentRound, int cardBonusPercentage) {
        int baseReward = 100;
        int roundBonus = currentRound * 10;
        int totalBeforeBonus = baseReward + roundBonus;

        // Apply percentage bonus
        int bonusAmount = (int) Math.floor(totalBeforeBonus * (cardBonusPercentage / 100.0));

        int finalReward = totalBeforeBonus + bonusAmount;

        logger.info("Gold calculation - Round {}: Base={}, Bonus={}%, Final={}",
                currentRound, totalBeforeBonus, cardBonusPercentage, finalReward);

        return finalReward;
    }

    /**
     * Award gold at round completion with full persistence
     * CRITICAL: This preserves existing gold and adds the reward
     */
    @Transactional
    public RPGGameState awardRoundGold(String gameId, String playerId) {
        RPGGameState gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game state not found: " + gameId));

        // Validate player
        validatePlayer(gameState, playerId);

        // Store current gold before calculation
        int currentGold = gameState.getCoins();

        // Calculate bonuses
        int cardBonus = calculateCardBonus(gameState);
        int rewardAmount = calculateRoundReward(gameState.getCurrentRound(), cardBonus);

        // Add to existing gold (CRITICAL: Don't replace, add)
        int newTotal = currentGold + rewardAmount;
        gameState.setCoins(newTotal);

        logger.info("[GOLD] Round completion award - Player {}: {} + {} = {} ({}% bonus)",
                playerId, currentGold, rewardAmount, newTotal, cardBonus);

        return rpgGameStateRepository.save(gameState);
    }

    /**
     * Safely update coins with validation
     * Supports both positive (awards) and negative (purchases) amounts
     */
    @Transactional
    public RPGGameState updateCoins(String gameId, int coinsToAdd, String playerId) {
        RPGGameState gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game state not found: " + gameId));

        // Validate player
        validatePlayer(gameState, playerId);

        int currentCoins = gameState.getCoins();
        int newTotal = currentCoins + coinsToAdd;

        // Prevent negative gold
        if (newTotal < 0) {
            throw new IllegalArgumentException(
                    String.format("Insufficient gold. Required: %d, Available: %d",
                            Math.abs(coinsToAdd), currentCoins)
            );
        }

        gameState.setCoins(newTotal);

        logger.info("[GOLD] Update - Player {}: {} {} {} = {}",
                playerId, currentCoins,
                coinsToAdd >= 0 ? "+" : "",
                coinsToAdd, newTotal);

        return rpgGameStateRepository.save(gameState);
    }

    /**
     * Verify gold persistence after state changes
     */
    public int verifyGoldPersistence(String gameId) {
        Optional<RPGGameState> stateOpt = rpgGameStateRepository.findById(gameId);
        if (stateOpt.isPresent()) {
            int coins = stateOpt.get().getCoins();
            logger.info("[GOLD] Verification - Game {}: {} coins", gameId, coins);
            return coins;
        }
        throw new RuntimeException("Game state not found for verification: " + gameId);
    }

    /**
     * Get gold breakdown for client display
     */
    public GoldBreakdown getGoldBreakdown(String gameId) {
        RPGGameState gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game state not found: " + gameId));

        int currentGold = gameState.getCoins();
        int cardBonus = calculateCardBonus(gameState);
        int nextRoundReward = calculateRoundReward(gameState.getCurrentRound() + 1, cardBonus);

        return new GoldBreakdown(currentGold, cardBonus, nextRoundReward);
    }

    /**
     * Validate player is part of the game
     */
    private void validatePlayer(RPGGameState gameState, String playerId) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("Player ID cannot be null or empty");
        }

        // For single player RPG, skip strict session checks
        if (gameState.getGameMode() == GameMode.SINGLE_PLAYER_RPG) {
            return;
        }

        // For multiplayer, validate session
        GameSession session = gameSessionRepository.findById(gameState.getGameSessionId())
                .orElseThrow(() -> new RuntimeException("Game session not found"));

        if (!session.getPlayerIds().contains(playerId)) {
            throw new RuntimeException("Player not in this game: " + playerId);
        }
    }

    /**
     * DTO for gold breakdown response
     */
    public static class GoldBreakdown {
        public final int currentGold;
        public final int cardBonusPercentage;
        public final int nextRoundReward;

        public GoldBreakdown(int currentGold, int cardBonusPercentage, int nextRoundReward) {
            this.currentGold = currentGold;
            this.cardBonusPercentage = cardBonusPercentage;
            this.nextRoundReward = nextRoundReward;
        }
    }
}