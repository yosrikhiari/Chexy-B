package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.AISystem.AIStrategy;
import org.example.chessmystic.Models.AISystem.EnemyArmyConfig;
import org.example.chessmystic.Models.GameStateandFlow.*;
import org.example.chessmystic.Models.Interactions.ActionType;
import org.example.chessmystic.Models.Mechanics.BoardConfiguration;
import org.example.chessmystic.Models.Mechanics.RPGGameState;
import org.example.chessmystic.Models.Mechanics.EnhancedGameState;
import org.example.chessmystic.Models.Mechanics.RPGRound;
import org.example.chessmystic.Models.Mechanics.RoundProgression;
import org.example.chessmystic.Models.Tracking.*;
import org.example.chessmystic.Models.UserManagement.User;
import org.example.chessmystic.Models.rpg.*;
import org.example.chessmystic.Models.Transactions.*;
import org.example.chessmystic.Repository.*;
import org.example.chessmystic.Repository.EnhancedGameStateRepository;
import org.example.chessmystic.Service.implementation.UserService;
import org.example.chessmystic.Service.interfaces.GameRelated.IPlayerActionService;
import org.example.chessmystic.Service.interfaces.GameRelated.IRPGGameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

// import org.example.chessmystic.Models.rpg.DreamerState;

@Service
public class RPGGameService implements IRPGGameService {
    private static final Logger logger = LoggerFactory.getLogger(RPGGameService.class);

    private final RPGGameStateRepository rpgGameStateRepository;
    private final UserService userService;
    private final GameSessionService gameSessionService;
    private final GameSessionRepository gameSessionRepository;
    private final GameSessionRepository gamesessionrepository;
    private final ShopItemRepository shopItemRepository;
    private final IPlayerActionService playerActionService;
    private final RPGRoundRepository rpgRoundRepository;
    private final EnemyArmyConfigRepository enemyArmyConfigRepository;
    private final BoardConfigurationRepository boardConfigurationRepository;
    private final PlayerPurchaseHistoryRepository playerPurchaseHistoryRepository;
    private final GameHistoryRepository gameHistoryRepository;
    private final GameResultRepository gameResultRepository;
    private final RoundProgressionRepository roundProgressionRepository;
    private final EnhancedGameStateRepository enhancedGameStateRepository;

    @Autowired
    public RPGGameService(RPGGameStateRepository rpgGameStateRepository,
                          UserService userService,
                          GameSessionService gameSessionService,
                          GameSessionRepository gameSessionRepository,
                          GameSessionRepository gamesessionrepository,
                          ShopItemRepository shopItemRepository,
                          IPlayerActionService playerActionService,
                          RPGRoundRepository rpgRoundRepository,
                          EnemyArmyConfigRepository enemyArmyConfigRepository,
                          BoardConfigurationRepository boardConfigurationRepository,
                          PlayerPurchaseHistoryRepository playerPurchaseHistoryRepository,
                          GameHistoryRepository gameHistoryRepository,
                          GameResultRepository gameResultRepository,
                          RPGPieceRepository rpgPieceRepository,
                          RPGModifierRepository rpgModifierRepository,
                          RoundProgressionRepository roundProgressionRepository,
                          EnhancedGameStateRepository enhancedGameStateRepository) {
        this.rpgGameStateRepository = rpgGameStateRepository;
        this.userService = userService;
        this.gameSessionService = gameSessionService;
        this.gameSessionRepository = gameSessionRepository;
        this.gamesessionrepository = gamesessionrepository;
        this.shopItemRepository = shopItemRepository;
        this.playerActionService = playerActionService;
        this.rpgRoundRepository = rpgRoundRepository;
        this.enemyArmyConfigRepository = enemyArmyConfigRepository;
        this.boardConfigurationRepository = boardConfigurationRepository;
        this.playerPurchaseHistoryRepository = playerPurchaseHistoryRepository;
        this.gameHistoryRepository = gameHistoryRepository;
        this.gameResultRepository = gameResultRepository;
        this.roundProgressionRepository = roundProgressionRepository;
        this.enhancedGameStateRepository = enhancedGameStateRepository;
    }

    @Override
    @Transactional
    public RPGGameState createRPGGame(String userId, String gameSessionId, boolean isMultiplayer) {
        validateUserAndSession(userId, gameSessionId);

        GameSession gameSession = gameSessionRepository.findById(gameSessionId)
                .orElseThrow(() -> new GameSessionNotFoundException(gameSessionId));

        validateGameMode(gameSession.getGameMode());

        if (gameSession.getGameMode() == GameMode.MULTIPLAYER_RPG) {
            validateMultiplayerSession(gameSession);
        }

        RoundProgression progression = roundProgressionRepository.findById("defaultProgressionId")
                .orElseThrow(() -> new ConfigurationNotFoundException("Round progression configuration not found"));

        int initialBoardSize = progression.getBaseBoardSize();
        RPGRound firstRound = getRoundConfiguration(1, initialBoardSize);
        BoardConfiguration boardConfig = getBoardConfiguration(1, initialBoardSize);
        EnemyArmyConfig enemyConfig = getEnemyConfiguration(1, 1);

        List<String> playerIds = new ArrayList<>();
        List<String> playerNames = new ArrayList<>();
        initializePlayerLists(userId, gameSession, playerIds, playerNames);

        RPGGameState rpgGameState = buildInitialGameState(
                gameSessionId,
                gameSession.getGameMode(),
                playerIds,
                playerNames,
                boardConfig,
                firstRound,
                enemyConfig
        );
        // Seed starter player army
        List<RPGPiece> starterArmy = generateStarterArmy();
        rpgGameState.setPlayerArmy(new ArrayList<>(starterArmy));

        if (enemyConfig.getPieces() != null && !enemyConfig.getPieces().isEmpty()) {
            rpgGameState.getEnemyArmyConfig().setPieces(new ArrayList<>(enemyConfig.getPieces()));
        }

        RPGGameState savedState = rpgGameStateRepository.save(rpgGameState);
        logger.info("Saved RPG game state with ID: {}", savedState.getGameId());
        
        updateGameSessionWithRpgState(gameSession, savedState);
        createGameHistory(gameSessionId, playerIds);

        // Create corresponding EnhancedGameState so Enhanced combat APIs work and seed starter army
        seedEnhancedGameState(savedState, starterArmy);

        logger.info("Created new RPG game for session: {} with gameId: {}", gameSessionId, savedState.getGameId());
        return savedState;
    }

    private void seedEnhancedGameState(RPGGameState baseState, List<RPGPiece> starterArmyBase) {
        // Cast starter army to enhanced for enhanced state
        List<EnhancedRPGPiece> starterArmy = new ArrayList<>();
        for (RPGPiece p : starterArmyBase) {
            if (p instanceof EnhancedRPGPiece) starterArmy.add((EnhancedRPGPiece) p);
        }
        EnhancedGameState enhanced = new EnhancedGameState();
        enhanced.setGameId(baseState.getGameId());
        enhanced.setGameSessionId(baseState.getGameSessionId());
        enhanced.setDifficulty(1);
        enhanced.setEnemyArmy(new ArrayList<>());
        enhanced.setAiStrategy(org.example.chessmystic.Models.AISystem.AIStrategy.BALANCED);
        enhanced.setTeleportPortalsnumber(1);
        enhanced.setRoundProgression(null);
        enhanced.setViewportSize(null);
        enhanced.setDragOffset(null);
        // Inherit core RPG state fields
        enhanced.setCurrentRound(baseState.getCurrentRound());
        enhanced.setPlayerArmy(new ArrayList<>(starterArmy));
        enhanced.setActiveModifiers(baseState.getActiveModifiers());
        enhanced.setActiveBoardModifiers(baseState.getActiveBoardModifiers());
        enhanced.setActiveCapacityModifiers(baseState.getActiveCapacityModifiers());
        enhanced.setBoardEffects(baseState.getBoardEffects());
        enhanced.setBoardSize(baseState.getBoardSize());
        enhanced.setArmyCapacity(baseState.getArmyCapacity());
        enhanced.setCompletedRounds(baseState.getCompletedRounds());
        enhanced.setLives(baseState.getLives());
        enhanced.setScore(baseState.getScore());
        enhanced.setCoins(baseState.getCoins());
        enhanced.setGameOver(baseState.isGameOver());
        enhanced.setCurrentObjective(baseState.getCurrentObjective());
        enhanced.setTurnsRemaining(baseState.getTurnsRemaining());
        enhanced.setCreatedAt(baseState.getCreatedAt());
        enhanced.setLastUpdated(baseState.getLastUpdated());
        enhanced.setLastPlayerActivity(baseState.getLastPlayerActivity());
        enhanced.setGameMode(baseState.getGameMode());
        enhanced.setStatus(baseState.getStatus());
        enhanced.setPlayerTurn(baseState.isPlayerTurn());
        enhanced.setCurrentRoundConfigId(baseState.getCurrentRoundConfigId());
        enhanced.setActionHistoryIds(baseState.getActionHistoryIds());
        enhanced.setShopStateId(baseState.getShopStateId());
        enhanced.setEnemyArmy(baseState.getEnemyArmyConfig() != null ? baseState.getEnemyArmyConfig().getPieces() : new ArrayList<>());
        enhanced.setQuests(baseState.getQuests());
        enhanced.setTieResolutionRequested(baseState.isTieResolutionRequested());
        enhanced.setTieOptions(baseState.getTieOptions());
        enhanced.setTieChosenByPlayerId(baseState.getTieChosenByPlayerId());
        enhanced.setMusicCueId(baseState.getMusicCueId());

        // Save with same id as RPG game so combat API uses rpgGameId
        EnhancedGameState savedEnhanced = enhancedGameStateRepository.save(enhanced);
        logger.info("Created Enhanced game state with ID: {} for RPG game: {}", savedEnhanced.getGameId(), baseState.getGameId());
    }

    private List<RPGPiece> generateStarterArmy() {
        List<RPGPiece> army = new ArrayList<>();
        EnhancedRPGPiece knight = new EnhancedRPGPiece(
                new RPGPiece(UUID.randomUUID().toString(), org.example.chessmystic.Models.chess.PieceType.KNIGHT,
                        org.example.chessmystic.Models.chess.PieceColor.white,
                        "Knight", "Brave knight", null, 10, 10, 3, 2, Rarity.COMMON, false),
                10, 1, 0);
        EnhancedRPGPiece pawn = new EnhancedRPGPiece(
                new RPGPiece(UUID.randomUUID().toString(), org.example.chessmystic.Models.chess.PieceType.PAWN,
                        org.example.chessmystic.Models.chess.PieceColor.white,
                        "Pawn", "Loyal pawn", null, 6, 6, 1, 1, Rarity.COMMON, false),
                6, 1, 0);
        army.add(knight);
        army.add(pawn);
        return army;
    }

    private void validateUserAndSession(String userId, String gameSessionId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (gameSessionId == null || gameSessionId.isBlank()) {
            throw new IllegalArgumentException("Game session ID cannot be null or empty");
        }
    }

    private void validateGameMode(GameMode gameMode) {
        if (gameMode != GameMode.SINGLE_PLAYER_RPG &&
                gameMode != GameMode.MULTIPLAYER_RPG ) {
            throw new IllegalArgumentException("Game session is not in RPG mode");
        }
    }

    private void validateMultiplayerSession(GameSession gameSession) {
        if (gameSession.getBlackPlayer() == null) {
            throw new IllegalStateException("Multiplayer RPG requires at least one black player");
        }
    }

    private RPGRound getRoundConfiguration(int roundNumber, int boardSize) {
        return rpgRoundRepository.findByRoundNumber(String.valueOf(roundNumber))
                .orElseGet(() -> generateDefaultRoundConfig(roundNumber, boardSize));
    }

    private RPGRound generateDefaultRoundConfig(int roundNumber, int boardSize) {
        return RPGRound.builder()
                .roundNumber(String.valueOf(roundNumber))
                .boardSize(boardSize)
                .objective(GameObjective.CHECKMATE)
                .turnLimit(50)
                .isBossRound(false)
                .coinsReward(100)
                .build();
    }

    private BoardConfiguration getBoardConfiguration(int round, int boardSize) {
        return boardConfigurationRepository.findByRoundAndBoardSize(round, boardSize)
                .orElseGet(() -> generateDefaultBoardConfig(round, boardSize));
    }

    private BoardConfiguration generateDefaultBoardConfig(int round, int boardSize) {
        return BoardConfiguration.builder()
                .round(round)
                .boardSize(boardSize)
                .effects(new ArrayList<>())
                .build();
    }

    private EnemyArmyConfig getEnemyConfiguration(int round, int difficulty) {
        return enemyArmyConfigRepository.findByRoundAndDifficulty(round, difficulty)
                .orElseGet(() -> generateDefaultEnemyConfig(round, difficulty));
    }

    private EnemyArmyConfig generateDefaultEnemyConfig(int round, int difficulty) {
        List<EnhancedRPGPiece> pieces = new ArrayList<>();
        // Placeholder: Add basic enemy pieces based on round and difficulty
        return EnemyArmyConfig.builder()
                .round(round)
                .difficulty(difficulty)
                .pieces(pieces)
                .strategy(AIStrategy.BALANCED)
                .queenExposed(false)
                .build();
    }

    private void initializePlayerLists(String userId, GameSession gameSession,
                                       List<String> playerIds, List<String> playerNames) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        playerIds.add(userId);
        playerNames.add(user.getFirstName() + " " + user.getLastName());

        if (gameSession.getGameMode() == GameMode.MULTIPLAYER_RPG) {
            if (gameSession.getBlackPlayer() != null && gameSession.getBlackPlayer().isConnected()) {
                playerIds.add(gameSession.getBlackPlayer().getUserId());
                playerNames.add(gameSession.getBlackPlayer().getDisplayName());
            }
            if (gameSession.getOtherPlayers() != null) {
                gameSession.getOtherPlayers().stream()
                        .filter(PlayerSessionInfo::isConnected)
                        .forEach(player -> {
                            playerIds.add(player.getUserId());
                            playerNames.add(player.getDisplayName());
                        });
            }
        }
    }

    private RPGGameState buildInitialGameState(String gameSessionId, GameMode gameMode,
                                               List<String> playerIds, List<String> playerNames,
                                               BoardConfiguration boardConfig, RPGRound firstRound,
                                               EnemyArmyConfig firstEnemyArmy) {
        ArmyCapacity defaultCapacity = ArmyCapacity.builder()
                .maxTotalPieces(16)
                .maxQueens(2)
                .maxRooks(4)
                .maxBishops(4)
                .maxKnights(4)
                .maxPawns(8)
                .bonusCapacity(0)
                .build();

        return RPGGameState.builder()
                .gameId(UUID.randomUUID().toString())
                .gameSessionId(gameSessionId)
                .playerArmy(new ArrayList<>())
                .enemyArmyConfig(firstEnemyArmy)
                .activeModifiers(new ArrayList<>())
                .activeBoardModifiers(new ArrayList<>())
                .activeCapacityModifiers(new ArrayList<>())
                .boardEffects(boardConfig.getEffects() != null ?
                        new ArrayList<>(boardConfig.getEffects()) : new ArrayList<>())
                .boardSize(boardConfig.getBoardSize())
                .armyCapacity(defaultCapacity)
                .lives(3)
                .score(0)
                .coins(100)
                .isGameOver(false)
                .gameMode(gameMode)
                .status(GameStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .lastPlayerActivity(LocalDateTime.now())
                .currentRound(1)
                .currentRoundConfigId(firstRound.getRoundNumber())
                .actionHistoryIds(new ArrayList<>())
                .currentObjective(firstRound.getObjective().toString())
                .turnsRemaining(firstRound.getTurnLimit())
                .completedRounds(new ArrayList<>()) // Initialize completedRounds here
                .build();
    }

    private void updateGameSessionWithRpgState(GameSession gameSession, RPGGameState rpgGameState) {
        gameSession.setRpgGameStateId(rpgGameState.getGameId());
        gameSessionService.updateGameStatus(gameSession.getGameId(), GameStatus.ACTIVE);
    }

    private void createGameHistory(String gameSessionId, List<String> playerIds) {
        GameHistory gameHistory = GameHistory.builder()
                .id(UUID.randomUUID().toString())
                .gameSessionId(gameSessionId)
                .userIds(playerIds)
                .isRPGMode(true)
                .startTime(LocalDateTime.now())
                .playerActionIds(new ArrayList<>())
                .build();
        gameHistoryRepository.save(gameHistory);
    }

    @Override
    public Optional<RPGGameState> findById(String gameId) {
        return rpgGameStateRepository.findById(gameId);
    }

    @Override
    public RPGGameState findByGameSessionId(String gameSessionId) {
        return rpgGameStateRepository.findByGameSessionId(gameSessionId)
                .orElseThrow(() -> new GameStateNotFoundException(gameSessionId));
    }

    @Override
    @Transactional
    public RPGGameState progressToNextRound(String gameId) {
        RPGGameState gameState = getGameState(gameId);
        validateGameNotOver(gameState);

        int currentCoins = gameState.getCoins();

        int nextRound = gameState.getCurrentRound() + 1;

        RoundProgression progression = roundProgressionRepository.findById("defaultProgressionId")
                .orElseThrow(() -> new ConfigurationNotFoundException("Round progression configuration not found"));

        int bossRoundsCompleted = (int) (gameState.getCompletedRounds() != null ?
                gameState.getCompletedRounds().stream()
                        .filter(r -> rpgRoundRepository.findByRoundNumber(String.valueOf(r))
                                .map(RPGRound::isBossRound)
                                .orElse(false))
                        .count() : 0);

        int boardSize = progression.getBaseBoardSize() + (bossRoundsCompleted * progression.getSizeIncreasePerBoss());
        RPGRound roundConfig = getRoundConfiguration(nextRound, boardSize);

        double difficultyMultiplier = progression.getDifficultyMultiplier();
        int difficulty = calculateDifficulty(nextRound, difficultyMultiplier);

        BoardConfiguration boardConfig = getBoardConfiguration(nextRound, boardSize);


        EnemyArmyConfig enemyConfig = getEnemyConfiguration(nextRound, difficulty);

        // Clear old enemy army and add new pieces
        gameState.getEnemyArmyConfig().getPieces().clear();
        if (enemyConfig.getPieces() != null && !enemyConfig.getPieces().isEmpty()) {
            gameState.getEnemyArmyConfig().getPieces().addAll(enemyConfig.getPieces());
        } else {
            // Generate default enemy army if config is empty
            List<EnhancedRPGPiece> defaultEnemies = generateDefaultEnemyArmy(nextRound, difficulty, boardSize);
            gameState.getEnemyArmyConfig().getPieces().addAll(defaultEnemies);
        }

        if (roundConfig.isBossRound()) {
            adjustEnemyConfigForBossRound(enemyConfig);
        }

        updateGameStateForNextRound(gameState, nextRound, roundConfig, boardConfig, enemyConfig);
        gameState.setCoins(currentCoins);

        if (gameState.getGameMode() == GameMode.MULTIPLAYER_RPG) {
            rotatePlayerTurn(gameState);
        }

        recordRoundProgressAction(gameState);

        return rpgGameStateRepository.save(gameState);
    }

    private List<EnhancedRPGPiece> generateDefaultEnemyArmy(int round, int difficulty, int boardSize) {
        List<EnhancedRPGPiece> enemies = new ArrayList<>();
        int enemyCount = Math.min(boardSize / 2, 4 + round); // Scale with round

        for (int i = 0; i < enemyCount; i++) {
            int baseHp = 10 + (round * 2);
            int baseAtk = 3 + round;
            int baseDef = 2 + round;

            EnhancedRPGPiece enemy = new EnhancedRPGPiece(
                    new RPGPiece(
                            UUID.randomUUID().toString(),
                            org.example.chessmystic.Models.chess.PieceType.values()[i % 6],
                            org.example.chessmystic.Models.chess.PieceColor.black,
                            "Enemy " + (i + 1),
                            "Round " + round + " enemy",
                            null,
                            baseHp, baseHp, baseAtk, baseDef,
                            Rarity.COMMON,
                            false
                    ),
                    baseHp,
                    Math.max(1, round / 3),
                    0
            );
            enemies.add(enemy);
        }

        return enemies;
    }

    private int calculateDifficulty(int round, double multiplier) {
        return (int) Math.ceil(round * multiplier);
    }

    private void adjustEnemyConfigForBossRound(EnemyArmyConfig enemyConfig) {
        enemyConfig.getPieces().forEach(piece -> {
            piece.setAttack((int) (piece.getAttack() * 1.5));
            piece.setDefense((int) (piece.getDefense() * 1.5));
        });
    }

    private RPGGameState getGameState(String gameId) {
        return rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new GameStateNotFoundException(gameId));
    }

    private void validateGameNotOver(RPGGameState gameState) {
        if (gameState.isGameOver()) {
            throw new IllegalStateException("Game is already over");
        }
    }

    private void updateGameStateForNextRound(RPGGameState gameState, int nextRound,
                                             RPGRound roundConfig, BoardConfiguration boardConfig,
                                             EnemyArmyConfig enemyConfig) {
        gameState.setCurrentRound(nextRound);
        gameState.setLastUpdated(LocalDateTime.now());
        gameState.setCurrentRoundConfigId(roundConfig.getRoundNumber());
        gameState.setCurrentObjective(roundConfig.getObjective().toString());
        gameState.setTurnsRemaining(roundConfig.getTurnLimit());
        gameState.setBoardSize(boardConfig.getBoardSize());
        gameState.setBoardEffects(boardConfig.getEffects() != null ?
                new ArrayList<>(boardConfig.getEffects()) : new ArrayList<>());

        if (gameState.getCompletedRounds() == null) {
            gameState.setCompletedRounds(new ArrayList<>());
        }
        gameState.getCompletedRounds().add(gameState.getCurrentRound() - 1);

        gameState.getEnemyArmyConfig().setPieces(new ArrayList<>());
        if (enemyConfig.getPieces() != null) {
            gameState.getEnemyArmyConfig().setPieces(new ArrayList<>(enemyConfig.getPieces()));
        }
    }

    private void recordRoundProgressAction(RPGGameState gameState) {
        GameSession gameSession = gamesessionrepository.findById(gameState.getGameSessionId())
                .orElse(null);

        playerActionService.recordAction(
                gameState.getGameSessionId(),
                gameSession.getCurrentPlayerId(),
                ActionType.NORMAL,
                -1, -1, -1, -1,
                null,
                gameState.getGameId(),
                gameState.getCurrentRound(),
                "ProgressToRound:" + gameState.getCurrentRound(),
                0,
                false,
                false
        );
    }

    // --- NEW: MVP progression and interactions ---

    @Override
    @Transactional
    public RPGGameState chooseSpecialization(String gameId, String pieceId, SpecializationType specialization, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);
        if (gameState.getPlayerArmy() == null) return gameState;
        for (RPGPiece p : gameState.getPlayerArmy()) {
            if (pieceId.equals(p.getId()) && p instanceof EnhancedRPGPiece) {
                EnhancedRPGPiece ep = (EnhancedRPGPiece) p;
                ep.setSpecialization(specialization);
                // Apply simple passive perks for MVP
                switch (specialization) {
                    case PALADIN_KNIGHT -> ep.setDefense(ep.getDefense() + 1);
                    case SHADOW_KNIGHT -> ep.setAttack(ep.getAttack() + 1);
                    default -> {}
                }
                // Grant abilities for MVP
                if (specialization == SpecializationType.PALADIN_KNIGHT) {
                    ep.getAbilities().add(AbilityId.HEAL_2);
                } else if (specialization == SpecializationType.SHADOW_KNIGHT) {
                    ep.getAbilities().add(AbilityId.SHADOW_STEP);
                }
                updateGameStateAndRotateTurn(gameState, playerId, "ChooseSpecialization:" + specialization);
                return rpgGameStateRepository.save(gameState);
            }
        }
        return gameState;
    }

    @Override
    @Transactional
    public RPGGameState activateAbility(String gameId, String pieceId, AbilityId abilityId, String targetPieceId, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);
        if (gameState.getPlayerArmy() == null) return gameState;
        for (RPGPiece p : gameState.getPlayerArmy()) {
            if (pieceId.equals(p.getId()) && p instanceof EnhancedRPGPiece) {
                EnhancedRPGPiece ep = (EnhancedRPGPiece) p;
                if (!ep.getAbilities().contains(abilityId)) {
                    throw new IllegalStateException("Ability not known by this piece");
                }
                Integer cd = ep.getCooldowns().getOrDefault(abilityId, 0);
                if (cd != null && cd > 0) {
                    throw new IllegalStateException("Ability is on cooldown");
                }

                switch (abilityId) {
                    case HEAL_2 -> {
                        // Heal adjacent ally by 2 HP (MVP: targetPieceId must be in army)
                        RPGPiece target = gameState.getPlayerArmy().stream()
                                .filter(t -> t.getId().equals(targetPieceId))
                                .findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid target"));
                        if (target instanceof EnhancedRPGPiece et) {
                            int before = et.getCurrentHp();
                            et.setCurrentHp(Math.min(et.getMaxHp(), et.getCurrentHp() + 2));
                            ep.getCooldowns().put(abilityId, 1); // once per round cooldown
                            updateGameStateAndRotateTurn(gameState, playerId, "Ability:HEAL_2:" + pieceId + ":" + targetPieceId + ":" + before + ">" + et.getCurrentHp());
                            return rpgGameStateRepository.save(gameState);
                        }
                    }
                    case SHADOW_STEP -> {
                        // MVP: record intent only; movement resolution happens elsewhere
                        ep.getTags().add("SHADOW_STEPPED");
                        ep.getCooldowns().put(abilityId, 1);
                        updateGameStateAndRotateTurn(gameState, playerId, "Ability:SHADOW_STEP:" + pieceId);
                        return rpgGameStateRepository.save(gameState);
                    }
                }
            }
        }
        return gameState;
    }

    @Override
    @Transactional
    public RPGGameState equipItem(String gameId, String pieceId, EquipmentItem item, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);
        if (gameState.getPlayerArmy() == null) return gameState;
        for (RPGPiece p : gameState.getPlayerArmy()) {
            if (pieceId.equals(p.getId())) {
                // Apply stat deltas
                p.setAttack(p.getAttack() + item.getAttackDelta());
                p.setDefense(p.getDefense() + item.getDefenseDelta());
                p.setMaxHp(Math.max(1, p.getMaxHp() + item.getMaxHpDelta()));
                if (p instanceof EnhancedRPGPiece ep && item.getTags() != null) {
                    ep.getTags().addAll(item.getTags());
                    // Transformations MVP for Knight
                    if (p.getType() == org.example.chessmystic.Models.chess.PieceType.KNIGHT) {
                        boolean hasHeroic = ep.getTags().contains("heroic");
                        boolean hasMalicious = ep.getTags().contains("malicious");
                        if (item.getName() != null && item.getName().toLowerCase().contains("sword") && hasHeroic) {
                            p.setName("Hero");
                        } else if (item.getName() != null && item.getName().toLowerCase().contains("sword")) {
                            p.setName("Swordsman");
                        }
                        if (hasMalicious) {
                            p.setName("Headless Knight");
                        }
                    }
                }
                updateGameStateAndRotateTurn(gameState, playerId, "EquipItem:" + item.getName());
                return rpgGameStateRepository.save(gameState);
            }
        }
        return gameState;
    }

    @Override
    @Transactional
    public RPGGameState resolveTie(String gameId, String playerId, String choice) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);
        // Only allow if tie requested and player has a Joker piece in army
        boolean hasJoker = gameState.getPlayerArmy() != null && gameState.getPlayerArmy().stream().anyMatch(RPGPiece::isJoker);
        if (gameState.isTieResolutionRequested() && hasJoker && gameState.getTieOptions() != null && gameState.getTieOptions().contains(choice)) {
            gameState.setTieChosenByPlayerId(playerId);
            gameState.setTieResolutionRequested(false);
            updateGameStateAndRotateTurn(gameState, playerId, "ResolveTie:" + choice);
            return rpgGameStateRepository.save(gameState);
        }
        return gameState;
    }

    @Override
    @Transactional
    public RPGGameState spawnQuests(String gameId, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);
        if (gameState.getQuests() == null) gameState.setQuests(new ArrayList<>());
        // Simple per-round per-player quest
        Quest q = Quest.builder()
                .id(UUID.randomUUID().toString())
                .type(QuestType.CAPTURE_N_PIECES)
                .assignedToPlayerId(playerId)
                .description("Capture 1 piece this round")
                .target(1)
                .progress(0)
                .completed(false)
                .coinsReward(50)
                .build();
        gameState.getQuests().add(q);
        updateGameStateAndRotateTurn(gameState, playerId, "SpawnQuest:" + q.getId());
        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState acceptQuest(String gameId, String questId, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);
        // For MVP, accepting is implicit; just ensure quest exists
        updateGameStateAndRotateTurn(gameState, playerId, "AcceptQuest:" + questId);
        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState completeQuest(String gameId, String questId, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);
        if (gameState.getQuests() != null) {
            gameState.getQuests().stream()
                    .filter(q -> questId.equals(q.getId()) && playerId.equals(q.getAssignedToPlayerId()))
                    .findFirst()
                    .ifPresent(q -> {
                        if (!q.isCompleted()) {
                            q.setCompleted(true);
                            gameState.setCoins(gameState.getCoins() + Math.max(0, q.getCoinsReward()));
                        }
                    });
        }
        updateGameStateAndRotateTurn(gameState, playerId, "CompleteQuest:" + questId);
        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState awardXp(String gameId, String pieceId, int xp, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);
        if (gameState.getPlayerArmy() == null) return gameState;
        for (RPGPiece p : gameState.getPlayerArmy()) {
            if (pieceId.equals(p.getId()) && p instanceof EnhancedRPGPiece) {
                EnhancedRPGPiece ep = (EnhancedRPGPiece) p;
                ep.setExperience(Math.max(0, ep.getExperience() + Math.max(0, xp)));
                // Level-up rule: 100 + 25*(level-1)
                boolean leveled = false;
                while (ep.getExperience() >= (100 + 25 * (ep.getLevel() - 1)) && ep.getLevel() < 100) {
                    ep.setExperience(ep.getExperience() - (100 + 25 * (ep.getLevel() - 1)));
                    ep.setLevel(ep.getLevel() + 1);
                    ep.setMaxHp(ep.getMaxHp() + 1);
                    ep.setAttack(ep.getAttack() + 1);
                    leveled = true;
                }
                updateGameStateAndRotateTurn(gameState, playerId, leveled ? "LevelUp:" + ep.getLevel() : "AwardXp:" + xp);
                return rpgGameStateRepository.save(gameState);
            }
        }
        return gameState;
    }

    @Override
    @Transactional
    public RPGGameState converseWithDreamer(String gameId, String pieceId, String prompt, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);
        if (gameState.getPlayerArmy() == null) return gameState;
        for (RPGPiece p : gameState.getPlayerArmy()) {
            if (pieceId.equals(p.getId()) && p instanceof EnhancedRPGPiece) {
                EnhancedRPGPiece ep = (EnhancedRPGPiece) p;
                ep.setConversationPrompt(prompt);
                // Simple rules MVP: contains positive words => INSPIRED; negative => DOUBTFUL; else DORMANT
                String lower = prompt == null ? "" : prompt.toLowerCase();
                if (lower.contains("hope") || lower.contains("brave") || lower.contains("dream")) {
                    ep.setDreamerState(org.example.chessmystic.Models.rpg.DreamerState.INSPIRED);
                } else if (lower.contains("fear") || lower.contains("doubt") || lower.contains("lose")) {
                    ep.setDreamerState(org.example.chessmystic.Models.rpg.DreamerState.DOUBTFUL);
                } else {
                    ep.setDreamerState(org.example.chessmystic.Models.rpg.DreamerState.DORMANT);
                }
                updateGameStateAndRotateTurn(gameState, playerId, "DreamerConverse:" + ep.getDreamerState());
                return rpgGameStateRepository.save(gameState);
            }
        }
        return gameState;
    }

    @Override
    @Transactional
    public RPGGameState preacherControl(String gameId, String preacherPieceId, String targetEnemyPieceId, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);
        if (gameState.getPlayerArmy() == null) return gameState;
        for (RPGPiece p : gameState.getPlayerArmy()) {
            if (preacherPieceId.equals(p.getId()) && p instanceof EnhancedRPGPiece) {
                EnhancedRPGPiece ep = (EnhancedRPGPiece) p;
                if (ep.getOncePerRunControlRemaining() == null || ep.getOncePerRunControlRemaining() <= 0) {
                    throw new IllegalStateException("Preacher control already used");
                }
                ep.setOncePerRunControlRemaining(ep.getOncePerRunControlRemaining() - 1);
                // MVP: Just record the intention; real control to be handled in move phase
                updateGameStateAndRotateTurn(gameState, playerId, "PreacherControl:" + targetEnemyPieceId);
                return rpgGameStateRepository.save(gameState);
            }
        }
        return gameState;
    }

    @Override
    @Transactional
    public RPGGameState triggerStatueEvent(String gameId, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);
        if (gameState.getQuests() == null) gameState.setQuests(new ArrayList<>());

        List<String> allPlayers = gamesessionrepository.findById(gameState.getGameSessionId())
                .map(GameSession::getPlayerIds)
                .orElse(List.of(playerId));

        Random rand = new Random();
        for (String pid : allPlayers) {
            Quest q = Quest.builder()
                    .id(UUID.randomUUID().toString())
                    .type(rand.nextBoolean() ? QuestType.SURVIVE_N_TURNS : QuestType.CAPTURE_N_PIECES)
                    .assignedToPlayerId(pid)
                    .description(rand.nextBoolean() ? "Survive 3 turns" : "Capture 1 piece")
                    .target(rand.nextBoolean() ? 3 : 1)
                    .progress(0)
                    .completed(false)
                    .coinsReward(50)
                    .build();
            gameState.getQuests().add(q);
        }
        updateGameStateAndRotateTurn(gameState, playerId, "StatueEvent:" + gameState.getCurrentRound());
        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState setMusicCue(String gameId, String cueId, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);
        gameState.setMusicCueId(cueId);
        updateGameStateAndRotateTurn(gameState, playerId, "MusicCue:" + cueId);
        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState updateWeaknesses(String gameId, String pieceId, java.util.Set<WeaknessType> weaknesses, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);
        if (gameState.getPlayerArmy() == null) return gameState;
        for (RPGPiece p : gameState.getPlayerArmy()) {
            if (pieceId.equals(p.getId()) && p instanceof EnhancedRPGPiece) {
                EnhancedRPGPiece ep = (EnhancedRPGPiece) p;
                ep.setWeaknesses(weaknesses != null ? weaknesses : new java.util.HashSet<>());
                // Derived tag: SEER if BLIND + CANT_SPEAK
                if (ep.getWeaknesses().contains(WeaknessType.BLIND) && ep.getWeaknesses().contains(WeaknessType.CANT_SPEAK)) {
                    ep.getTags().add("SEER");
                }
                updateGameStateAndRotateTurn(gameState, playerId, "UpdateWeaknesses:" + ep.getWeaknesses().size());
                return rpgGameStateRepository.save(gameState);
            }
        }
        return gameState;
    }

    private void rotatePlayerTurn(RPGGameState gameState) {
        GameSession session = gameSessionRepository.findById(gameState.getGameSessionId())
                .orElseThrow(() -> new GameSessionNotFoundException(gameState.getGameSessionId()));

        List<String> activePlayerIds = session.getPlayerIds().stream()
                .filter(id -> isPlayerActive(session, id))
                .toList();

        if (!activePlayerIds.isEmpty()) {
            int currentIndex = activePlayerIds.indexOf(session.getCurrentPlayerId());
            int nextIndex = (currentIndex + 1) % activePlayerIds.size();
            session.setCurrentTurn(activePlayerIds.get(nextIndex));
            gameSessionRepository.save(session);
        }
    }

    private boolean isPlayerActive(GameSession session, String playerId) {
        return (session.getWhitePlayer() != null && session.getWhitePlayer().getUserId().equals(playerId) && session.getWhitePlayer().isConnected()) ||
                (session.getBlackPlayer() != null && session.getBlackPlayer().getUserId().equals(playerId) && session.getBlackPlayer().isConnected()) ||
                (session.getOtherPlayers() != null && session.getOtherPlayers().stream().anyMatch(p -> p.getUserId().equals(playerId) && p.isConnected()));
    }

    @Override
    @Transactional
    public RPGGameState addPieceToArmy(String gameId, RPGPiece piece, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);
        validateArmyAddition(gameState, piece);

        if (gameState.getPlayerArmy() == null) {
            gameState.setPlayerArmy(new ArrayList<>());
        }
        gameState.getPlayerArmy().add(piece);
        updateGameStateAndRotateTurn(gameState, playerId, "AddPiece:" + piece.getType());

        return rpgGameStateRepository.save(gameState);
    }

    private void validateArmyAddition(RPGGameState gameState, RPGPiece piece) {
        if (gameState.getArmyCapacity() != null) {
            validateArmyCapacity(gameState.getArmyCapacity(), piece, gameState.getPlayerArmy());
        }
    }

    @Override
    @Transactional
    public RPGGameState addModifier(String gameId, RPGModifier modifier, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);

        if (gameState.getActiveModifiers() == null) {
            gameState.setActiveModifiers(new ArrayList<>());
        }
        gameState.getActiveModifiers().add(modifier);
        updateGameStateAndRotateTurn(gameState, playerId, "AddModifier:" + modifier.getName());

        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState addBoardEffect(String gameId, BoardEffect effect, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);

        if (gameState.getBoardEffects() == null) {
            gameState.setBoardEffects(new ArrayList<>());
        }
        gameState.getBoardEffects().add(effect);
        updateGameStateAndRotateTurn(gameState, playerId, "AddBoardEffect:" + effect.getName());

        return rpgGameStateRepository.save(gameState);
    }

    private RPGGameState getValidatedGameState(String gameId, String playerId) {
        RPGGameState gameState = getGameState(gameId);
        validateGameAction(gameState, playerId);
        return gameState;
    }

    private void validateGameAction(RPGGameState gameState, String playerId) {
        if (gameState.isGameOver()) {
            throw new IllegalStateException("Cannot perform action in a completed game");
        }

        GameSession session = gameSessionRepository.findById(gameState.getGameSessionId())
                .orElseThrow(() -> new GameSessionNotFoundException(gameState.getGameSessionId()));

        // For single player RPG, be more lenient with player validation
        if (gameState.getGameMode() == GameMode.SINGLE_PLAYER_RPG) {
            // Only check if any player in session matches - don't require exact turn matching
            boolean isPlayerInGame = session.getPlayerIds().contains(playerId) ||
                    (session.getWhitePlayer() != null && session.getWhitePlayer().getUserId().equals(playerId)) ||
                    (session.getBlackPlayer() != null && session.getBlackPlayer().getUserId().equals(playerId));

            if (!isPlayerInGame) {
                logger.error("Player {} not found in single player game. Session players: {}",
                        playerId, session.getPlayerIds());
                throw new PlayerNotInGameException(playerId);
            }
            // Skip turn validation for single player
            return;
        }

        // Original multiplayer validation
        if (!session.getPlayerIds().contains(playerId)) {
            throw new PlayerNotInGameException(playerId);
        }

        if (!session.getCurrentPlayerId().equals(playerId)) {
            throw new NotPlayerTurnException(playerId);
        }
    }

    private void updateGameStateAndRotateTurn(RPGGameState gameState, String playerId, String actionDescription) {
        gameState.setLastUpdated(LocalDateTime.now());

        if (gameState.getGameMode() == GameMode.MULTIPLAYER_RPG) {
            rotatePlayerTurn(gameState);
        }

        recordGenericAction(gameState, playerId, actionDescription);
    }

    private void recordGenericAction(RPGGameState gameState, String playerId, String description) {
        playerActionService.recordAction(
                gameState.getGameSessionId(),
                playerId,
                ActionType.NORMAL,
                -1, -1, -1, -1,
                null,
                gameState.getGameId(),
                gameState.getCurrentRound(),
                description,
                0,
                false,
                false
        );
    }

    @Override
    @Transactional
    public RPGGameState updateScore(String gameId, int scoreToAdd, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);

        gameState.setScore(gameState.getScore() + scoreToAdd);
        updateGameStateAndRotateTurn(gameState, playerId, "UpdateScore:" + scoreToAdd);

        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState updateCoins(String gameId, int coinsToAdd, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);

        gameState.setCoins(gameState.getCoins() + coinsToAdd);
        updateGameStateAndRotateTurn(gameState, playerId, "UpdateCoins:" + coinsToAdd);

        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState endGame(String gameId, boolean victory) {
        RPGGameState gameState = getGameState(gameId);

        if (gameState.isGameOver()) {
            return gameState;
        }

        gameState.setGameOver(true);
        gameState.setStatus(victory ? GameStatus.COMPLETED : GameStatus.ABANDONED);
        gameState.setLastUpdated(LocalDateTime.now());

        recordGameEndAction(gameState, victory);
        updateGameHistory(gameState, victory);
        createGameResult(gameState, victory);
        endGameSession(gameState, victory);

        return rpgGameStateRepository.save(gameState);
    }

    private void recordGameEndAction(RPGGameState gameState, boolean victory) {
        GameSession gameSession = gamesessionrepository.findById(gameState.getGameSessionId())
                .orElse(null);
        playerActionService.recordAction(
                gameState.getGameSessionId(),
                gameSession.getCurrentPlayerId(),
                ActionType.NORMAL,
                -1, -1, -1, -1,
                null,
                gameState.getGameId(),
                gameState.getCurrentRound(),
                "EndGame:" + (victory ? "Victory" : "Abandoned"),
                0,
                false,
                false
        );
    }

    private void updateGameHistory(RPGGameState gameState, boolean victory) {
        GameHistory gameHistory = gameHistoryRepository.findByGameSessionId(gameState.getGameSessionId())
                .orElseThrow(() -> new GameHistoryNotFoundException(gameState.getGameSessionId()));

        gameHistory.setEndTime(LocalDateTime.now());
        gameHistory.setFinalRound(gameState.getCurrentRound());
        gameHistory.setFinalScore(gameState.getScore());
        gameHistoryRepository.save(gameHistory);
    }

    private void createGameResult(RPGGameState gameState, boolean victory) {
        GameSession gameSession = gamesessionrepository.findById(gameState.getGameSessionId())
                .orElse(null);

        GameResult gameResult = GameResult.builder()
                .gameresultId(UUID.randomUUID().toString())
                .gameid(gameState.getGameSessionId())
                .winnerid(victory ? Objects.requireNonNull(gameSession).getCurrentPlayerId() : null)
                .pointsAwarded(gameState.getScore())
                .gameEndReason(victory ? GameEndReason.checkmate : GameEndReason.resignation)
                .build();
        gameResultRepository.save(gameResult);
    }

    private void endGameSession(RPGGameState gameState, boolean victory) {
        GameSession gameSession = gamesessionrepository.findById(gameState.getGameSessionId())
                .orElse(null);
        gameSessionService.endGame(
                gameState.getGameSessionId(),
                victory ? Objects.requireNonNull(gameSession).getCurrentPlayerId() : null,
                false,
                null,
                GameEndReason.checkmate
        );
    }

    @Override
    public List<RPGGameState> findActiveGamesByUser(String userId) {
        return rpgGameStateRepository.findActiveGamesByUserId(userId);
    }

    @Override
    @Transactional
    public RPGGameState purchaseShopItem(String gameId, String shopItemId, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);
        ShopItem shopItem = getShopItem(shopItemId);

        validatePurchase(gameState, shopItem);

        processPurchase(gameState, shopItem);
        recordPurchaseHistory(gameState, playerId, shopItem);
        updateGameStateAndRotateTurn(gameState, playerId, "Purchase:" + shopItem.getName());

        return rpgGameStateRepository.save(gameState);
    }

    private ShopItem getShopItem(String shopItemId) {
        return shopItemRepository.findById(shopItemId)
                .orElseThrow(() -> new ShopItemNotFoundException(shopItemId));
    }

    private void validatePurchase(RPGGameState gameState, ShopItem shopItem) {
        if (gameState.getCoins() < shopItem.getCost()) {
            throw new InsufficientCoinsException(shopItem.getCost(), gameState.getCoins());
        }

        if ("piece".equals(shopItem.getType()) && shopItem.getItem() instanceof RPGPiece) {
            validateArmyAddition(gameState, (RPGPiece) shopItem.getItem());
        }
    }

    private void processPurchase(RPGGameState gameState, ShopItem shopItem) {
        gameState.setCoins(gameState.getCoins() - shopItem.getCost());

        if ("piece".equals(shopItem.getType())) {
            addPurchasedPiece(gameState, (RPGPiece) shopItem.getItem());
        } else if ("modifier".equals(shopItem.getType())) {
            addPurchasedModifier(gameState, (RPGModifier) shopItem.getItem());
        }
    }

    private void addPurchasedPiece(RPGGameState gameState, RPGPiece piece) {
        if (gameState.getPlayerArmy() == null) {
            gameState.setPlayerArmy(new ArrayList<>());
        }
        gameState.getPlayerArmy().add(piece);
    }

    private void addPurchasedModifier(RPGGameState gameState, RPGModifier modifier) {
        if (gameState.getActiveModifiers() == null) {
            gameState.setActiveModifiers(new ArrayList<>());
        }
        gameState.getActiveModifiers().add(modifier);
    }

    private void recordPurchaseHistory(RPGGameState gameState, String playerId, ShopItem shopItem) {
        PlayerPurchaseHistory purchaseHistory = PlayerPurchaseHistory.builder()
                .id(UUID.randomUUID().toString())
                .userId(playerId)
                .gameSessionId(gameState.getGameSessionId())
                .shopItems(List.of(shopItem))
                .costPaid(shopItem.getCost())
                .purchaseTime(LocalDateTime.now())
                .build();
        playerPurchaseHistoryRepository.save(purchaseHistory);
    }

    private void validateArmyCapacity(ArmyCapacity capacity, RPGPiece piece, List<RPGPiece> army) {
        if (capacity == null) return;

        long count = army != null ? army.stream().filter(p -> p.getType() == piece.getType()).count() : 0;

        switch (piece.getType()) {
            case QUEEN:
                validateCapacityLimit(count, capacity.getMaxQueens(), "Max queens limit exceeded");
                break;
            case ROOK:
                validateCapacityLimit(count, capacity.getMaxRooks(), "Max rooks limit exceeded");
                break;
            case BISHOP:
                validateCapacityLimit(count, capacity.getMaxBishops(), "Max bishops limit exceeded");
                break;
            case KNIGHT:
                validateCapacityLimit(count, capacity.getMaxKnights(), "Max knights limit exceeded");
                break;
            case PAWN:
                validateCapacityLimit(count, capacity.getMaxPawns(), "Max pawns limit exceeded");
                break;
            case KING:
                // No explicit cap for king in capacity
                break;
        }

        if (army != null && army.size() >= capacity.getMaxTotalPieces()) {
            throw new ArmyCapacityExceededException(capacity.getMaxTotalPieces());
        }
    }

    private void validateCapacityLimit(long currentCount, int maxLimit, String errorMessage) {
        if (currentCount >= maxLimit) {
            throw new ArmyCapacityExceededException(errorMessage);
        }
    }

    // Custom exceptions
    private static class GameSessionNotFoundException extends RuntimeException {
        public GameSessionNotFoundException(String gameSessionId) {
            super("Game session not found: " + gameSessionId);
        }
    }

    private static class GameStateNotFoundException extends RuntimeException {
        public GameStateNotFoundException(String gameId) {
            super("Game state not found: " + gameId);
        }
    }

    private static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String userId) {
            super("User not found: " + userId);
        }
    }

    private static class ConfigurationNotFoundException extends RuntimeException {
        public ConfigurationNotFoundException(String message) {
            super(message);
        }
    }

    private static class PlayerNotInGameException extends RuntimeException {
        public PlayerNotInGameException(String playerId) {
            super("Player not part of this game: " + playerId);
        }
    }

    private static class NotPlayerTurnException extends RuntimeException {
        public NotPlayerTurnException(String playerId) {
            super("Not player's turn: " + playerId);
        }
    }

    private static class GameHistoryNotFoundException extends RuntimeException {
        public GameHistoryNotFoundException(String gameSessionId) {
            super("Game history not found for session: " + gameSessionId);
        }
    }

    private static class ShopItemNotFoundException extends RuntimeException {
        public ShopItemNotFoundException(String shopItemId) {
            super("Shop item not found: " + shopItemId);
        }
    }

    private static class InsufficientCoinsException extends RuntimeException {
        public InsufficientCoinsException(int required, int available) {
            super(String.format("Insufficient coins. Required: %d, Available: %d", required, available));
        }
    }

    private static class ArmyCapacityExceededException extends RuntimeException {
        public ArmyCapacityExceededException(String message) {
            super(message);
        }

        public ArmyCapacityExceededException(int maxCapacity) {
            super("Max total army capacity exceeded: " + maxCapacity);
        }
    }

    @Transactional
    @Override
    public RPGGameState trackKill(String gameId, String killerPieceId, String playerId) {
        RPGGameState gameState = getValidatedGameState(gameId, playerId);

        if (gameState.getPlayerArmy() == null) return gameState;

        for (RPGPiece p : gameState.getPlayerArmy()) {
            if (killerPieceId.equals(p.getId()) && p instanceof EnhancedRPGPiece) {
                EnhancedRPGPiece ep = (EnhancedRPGPiece) p;

                int currentLevel = ep.getLevel();
                int killsNeeded = currentLevel; // Level 1 needs 1 kill, Level 2 needs 2 kills, etc.
                int currentKills = (ep.getKillCount() != null ? ep.getKillCount() : 0) + 1;

                ep.setKillCount(currentKills);

                // Check for level up
                if (currentKills >= killsNeeded) {
                    ep.setLevel(currentLevel + 1);
                    ep.setMaxHp(ep.getMaxHp() + 5);
                    ep.setCurrentHp(ep.getMaxHp()); // Full heal
                    ep.setAttack(ep.getAttack() + 2);
                    ep.setDefense(ep.getDefense() + 1);
                    ep.setKillCount(0); // Reset kill counter

                    updateGameStateAndRotateTurn(gameState, playerId,
                            "LevelUp:" + ep.getName() + ":Level" + ep.getLevel());
                } else {
                    updateGameStateAndRotateTurn(gameState, playerId,
                            "Kill:" + ep.getName() + ":" + currentKills + "/" + killsNeeded);
                }

                return rpgGameStateRepository.save(gameState);
            }
        }

        throw new IllegalArgumentException("Killer piece not found in player army: " + killerPieceId);
    }
}