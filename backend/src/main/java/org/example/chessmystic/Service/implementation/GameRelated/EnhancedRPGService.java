package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.AISystem.AIStrategy;
import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Models.GameStateandFlow.GameStatus;
import org.example.chessmystic.Models.Interactions.ActionType;
import org.example.chessmystic.Models.Interactions.CombatResult;
import org.example.chessmystic.Models.Mechanics.EnhancedGameState;
import org.example.chessmystic.Models.Mechanics.RPGBoss;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Models.Transactions.RPGModifier;
import org.example.chessmystic.Models.rpg.BoardEffect;
import org.example.chessmystic.Models.rpg.EnhancedRPGPiece;
import org.example.chessmystic.Repository.*;
import org.example.chessmystic.Service.interfaces.GameRelated.IEnhancedRPGService;
import org.example.chessmystic.Service.interfaces.GameRelated.IPlayerActionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EnhancedRPGService implements IEnhancedRPGService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedRPGService.class);

    private final IPlayerActionService playerActionService;
    private final EnhancedGameStateRepository enhancedGameStateRepository;
    private final GameSessionService gameSessionService;
    private final GameSessionRepository gamesessionrepository;
    private final RPGBossRepository rpgBossRepository;
    private final BoardEffectRepository boardEffectRepository;
    private final PlayerProfileRepository playerProfileRepository;

    @Autowired
    public EnhancedRPGService(IPlayerActionService playerActionService,
                              EnhancedGameStateRepository enhancedGameStateRepository,
                              GameSessionService gameSessionService, GameSessionRepository gamesessionrepository,
                              RPGBossRepository rpgBossRepository,
                              RPGModifierRepository rpgModifierRepository,
                              BoardEffectRepository boardEffectRepository,
                              RPGPieceRepository rpgPieceRepository,
                              PlayerProfileRepository playerProfileRepository) {
        this.playerActionService = playerActionService;
        this.enhancedGameStateRepository = enhancedGameStateRepository;
        this.gameSessionService = gameSessionService;
        this.gamesessionrepository = gamesessionrepository;
        this.rpgBossRepository = rpgBossRepository;
        this.boardEffectRepository = boardEffectRepository;
        this.playerProfileRepository = playerProfileRepository;
    }

    @Transactional
    @Override
    public CombatResult resolveCombat(EnhancedRPGPiece attacker, EnhancedRPGPiece defender, String gameId, String playerId) {
        EnhancedGameState gameState = enhancedGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Enhanced game state not found"));

        validatePlayerTurn(gameState, playerId);

        // Calculate bonuses from active modifiers
        int attackBonus = calculateAttackBonus(gameState);
        int defenseBonus = calculateDefenseBonus(gameState);

        // Calculate damage with level scaling
        int damage = calculateDamage(attacker, defender, attackBonus, defenseBonus);
        defender.setCurrentHp(defender.getCurrentHp() - damage);

        boolean defenderDefeated = defender.getCurrentHp() <= 0;
        Integer counterDamage = defenderDefeated ? null : calculateCounterDamage(defender, attacker);

        // Handle combat results
        handleCombatResults(gameState, attacker, defender, damage, defenderDefeated, counterDamage);

        // Check game over conditions
        checkGameOverConditions(gameState);

        // Handle turn rotation for multiplayer
        rotateTurnIfMultiplayer(gameState);

        // Record the combat action
        recordCombatAction(gameState, playerId, attacker, defender, damage, defenderDefeated);

        enhancedGameStateRepository.save(gameState);

        return buildCombatResult(attacker, defender, damage, defenderDefeated, counterDamage);
    }

    @Transactional
    @Override
    public void applyBoardEffect(String gameId, BoardEffect effect, String playerId) {
        EnhancedGameState gameState = enhancedGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game state not found"));

        validateGameActive(gameState);
        validatePlayerTurn(gameState, playerId);

        // Save and apply the board effect
        BoardEffect savedEffect = boardEffectRepository.save(effect);
        gameState.getBoardEffects().add(savedEffect);
        gameState.setLastUpdated(LocalDateTime.now());

        // Handle turn rotation for multiplayer
        rotateTurnIfMultiplayer(gameState);

        // Record the action
        recordBoardEffectAction(gameState, playerId, savedEffect);

        enhancedGameStateRepository.save(gameState);
    }

    @Transactional
    @Override
    public void handleBossEncounter(String gameId, RPGBoss boss, String playerId) {
        EnhancedGameState gameState = enhancedGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Enhanced game state not found"));

        validateGameActive(gameState);
        validatePlayerTurn(gameState, playerId);

        // Save or retrieve boss
        RPGBoss savedBoss = rpgBossRepository.findById(boss.getId())
                .orElseGet(() -> rpgBossRepository.save(boss));

        // Update game state with boss encounter
        updateGameStateForBossEncounter(gameState, savedBoss);

        // Handle turn rotation for multiplayer
        rotateTurnIfMultiplayer(gameState);

        // Record the action
        recordBossEncounterAction(gameState, playerId, savedBoss);

        enhancedGameStateRepository.save(gameState);
    }

    // Helper methods
    private void validatePlayerTurn(EnhancedGameState gameState, String playerId) {
        if (gamesessionrepository.findByEnhancedGameStateIdAndPlayerId(gameState.getGameId(), playerId) == null) {
            throw new RuntimeException("Player " + playerId + " is not part of this game");
        }
        var session = gamesessionrepository.findByEnhancedGameStateIdAndPlayerId(gameState.getGameId(), playerId);
        if (session == null || session.getCurrentPlayerId() == null || !session.getCurrentPlayerId().equals(playerId)) {
            throw new RuntimeException("Not your turn");
        }
    }

    private void validateGameActive(EnhancedGameState gameState) {
        if (gameState.isGameOver()) {
            throw new RuntimeException("Cannot perform action in a completed game");
        }
    }

    private int calculateAttackBonus(EnhancedGameState gameState) {
        return gameState.getActiveModifiers().stream()
                .filter(RPGModifier::isActive)
                .mapToInt(mod -> parseEffectValue(mod.getEffect(), "attack"))
                .sum();
    }

    private int calculateDefenseBonus(EnhancedGameState gameState) {
        return gameState.getActiveModifiers().stream()
                .filter(RPGModifier::isActive)
                .mapToInt(mod -> parseEffectValue(mod.getEffect(), "defense"))
                .sum();
    }

    private int calculateDamage(EnhancedRPGPiece attacker, EnhancedRPGPiece defender, int attackBonus, int defenseBonus) {
        return Math.max(1, (attacker.getAttack() + attackBonus + attacker.getLevel() * 2) -
                (defender.getDefense() + defenseBonus));
    }

    private int calculateCounterDamage(EnhancedRPGPiece defender, EnhancedRPGPiece attacker) {
        return Math.max(1, (defender.getAttack() + defender.getLevel() * 2) - attacker.getDefense());
    }

    private void handleCombatResults(EnhancedGameState gameState, EnhancedRPGPiece attacker,
                                     EnhancedRPGPiece defender, int damage, boolean defenderDefeated,
                                     Integer counterDamage) {
        if (defenderDefeated) {
            handleDefeatedPiece(gameState, defender, damage);
        }

        if (counterDamage != null && attacker.getCurrentHp() - counterDamage <= 0) {
            handleDefeatedPiece(gameState, attacker, 0);
        }
    }

    private void handleDefeatedPiece(EnhancedGameState gameState, EnhancedRPGPiece piece, int scoreValue) {
        if (gameState.getEnemyArmy().contains(piece)) {
            gameState.getEnemyArmy().remove(piece);
            gameState.setScore(gameState.getScore() + scoreValue * 10);
            gameState.setCoins(gameState.getCoins() + 5);

            // Get game session to access player IDs
            GameSession gameSession = gamesessionrepository.findById(gameState.getGameSessionId())
                    .orElse(null);
            if (gameSession != null) {
                // Update all players in the game (for team games)
                gameSession.getPlayerIds().forEach(playerId ->
                        updatePlayerProfile(playerId, scoreValue * 10, 5));
            }
        } else if (gameState.getPlayerArmy().contains(piece)) {
            gameState.getPlayerArmy().remove(piece);
            gameState.setLives(gameState.getLives() - 1);
        }
    }

    private void updatePlayerProfile(String userId, int score, int coins) {
        playerProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setTotalCoins(profile.getTotalCoins() + coins);
            profile.setHighestScore(Math.max(profile.getHighestScore(), score));
            playerProfileRepository.save(profile);
        });
    }

    private void checkGameOverConditions(EnhancedGameState gameState) {
        if (gameState.getLives() <= 0 || gameState.getEnemyArmy().isEmpty()) {
            boolean victory = gameState.getLives() > 0;
            gameState.setGameOver(true);
            gameState.setStatus(victory ? GameStatus.COMPLETED : GameStatus.ABANDONED);

            // Get winner ID for game end
            GameSession gameSession = gamesessionrepository.findById(gameState.getGameSessionId())
                    .orElse(null);
            String winnerId = victory && gameSession != null ? gameSession.getCurrentPlayerId() : null;

            gameSessionService.endGame(gameState.getGameSessionId(), winnerId, false, null);
        }
    }

    private void rotateTurnIfMultiplayer(EnhancedGameState gameState) {
        if (gameState.getGameMode() == GameMode.MULTIPLAYER_RPG
                ) {

            GameSession gameSession = gamesessionrepository.findById(gameState.getGameSessionId())
                    .orElse(null);

            if (gameSession != null) {
                List<String> playerIds = gameSession.getPlayerIds();
                String currentPlayerId = gameSession.getCurrentPlayerId();

                if (currentPlayerId != null && playerIds.size() > 1) {
                    int currentIndex = playerIds.indexOf(currentPlayerId);
                    int nextIndex = (currentIndex + 1) % playerIds.size();
                    String nextPlayerId = playerIds.get(nextIndex);

                    gameSession.setCurrentTurn(nextPlayerId);
                    gamesessionrepository.save(gameSession);
                }
            }
        }
    }

    private void recordCombatAction(EnhancedGameState gameState, String playerId,
                                    EnhancedRPGPiece attacker, EnhancedRPGPiece defender,
                                    int damage, boolean defenderDefeated) {
        playerActionService.recordAction(
                gameState.getGameSessionId(),
                playerId,
                ActionType.CAPTURE,
                -1, -1, -1, -1,
                null,
                gameState.getGameId(),
                gameState.getCurrentRound(),
                "Combat:" + attacker.getType() + " vs " + defender.getType(),
                damage,
                defenderDefeated,
                false
        );
    }

    private void recordBoardEffectAction(EnhancedGameState gameState, String playerId, BoardEffect effect) {
        playerActionService.recordAction(
                gameState.getGameSessionId(),
                playerId,
                ActionType.NORMAL,
                -1, -1, -1, -1,
                null,
                gameState.getGameId(),
                gameState.getCurrentRound(),
                "ApplyBoardEffect:" + effect.getName(),
                0,
                false,
                false
        );
    }

    private void recordBossEncounterAction(EnhancedGameState gameState, String playerId, RPGBoss boss) {
        playerActionService.recordAction(
                gameState.getGameSessionId(),
                playerId,
                ActionType.NORMAL,
                -1, -1, -1, -1,
                null,
                gameState.getGameId(),
                gameState.getCurrentRound(),
                "BossEncounter:" + boss.getName(),
                0,
                false,
                false
        );
    }

    private CombatResult buildCombatResult(EnhancedRPGPiece attacker, EnhancedRPGPiece defender,
                                           int damage, boolean defenderDefeated, Integer counterDamage) {
        return CombatResult.builder()
                .attacker(attacker)
                .defender(defender)
                .damage(damage)
                .defenderDefeated(defenderDefeated)
                .attackerCounterDamage(counterDamage)
                .build();
    }

    private void updateGameStateForBossEncounter(EnhancedGameState gameState, RPGBoss boss) {
        gameState.setCurrentObjective("Defeat " + boss.getName());
        if (gameState.getBossEncounterIds() == null) {
            gameState.setBossEncounterIds(new ArrayList<>());
        }
        gameState.getBossEncounterIds().add(UUID.randomUUID().toString());
        gameState.setDifficulty(gameState.getDifficulty() + 1);
        gameState.setAiStrategy(AIStrategy.AGGRESSIVE);

        if (boss.getPieces() != null && !boss.getPieces().isEmpty()) {
            if (gameState.getEnemyArmy() == null) {
                gameState.setEnemyArmy(new ArrayList<>());
            }
            boss.getPieces().forEach(piece ->
                    gameState.getEnemyArmy().add(new EnhancedRPGPiece(piece, piece.getMaxHp(), 1, 0))
            );
        }
    }

    private int parseEffectValue(String effect, String key) {
        if (effect == null) return 0;
        try {
            String[] parts = effect.split(":");
            for (int i = 0; i < parts.length - 1; i++) {
                if (parts[i].equals(key)) {
                    return Integer.parseInt(parts[i + 1]);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse effect value for key: " + key, e);
        }
        return 0;
    }
}