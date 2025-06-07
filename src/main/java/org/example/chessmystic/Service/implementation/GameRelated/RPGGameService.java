package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.AISystem.EnemyArmyConfig;
import org.example.chessmystic.Models.GameStateandFlow.*;
import org.example.chessmystic.Models.Interactions.ActionType;
import org.example.chessmystic.Models.Mechanics.BoardConfiguration;
import org.example.chessmystic.Models.Mechanics.RPGGameState;
import org.example.chessmystic.Models.Mechanics.RPGRound;
import org.example.chessmystic.Models.Tracking.*;
import org.example.chessmystic.Models.UserManagement.User;
import org.example.chessmystic.Models.rpg.*;
import org.example.chessmystic.Models.Transactions.*;
import org.example.chessmystic.Repository.*;
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
import java.util.stream.Collectors;

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
    private final RPGPieceRepository rpgPieceRepository;
    private final RPGModifierRepository rpgModifierRepository;

    @Autowired
    public RPGGameService(RPGGameStateRepository rpgGameStateRepository,
                          UserService userService,
                          GameSessionService gameSessionService,
                          GameSessionRepository gameSessionRepository, GameSessionRepository gamesessionrepository,
                          ShopItemRepository shopItemRepository,
                          IPlayerActionService playerActionService,
                          RPGRoundRepository rpgRoundRepository,
                          EnemyArmyConfigRepository enemyArmyConfigRepository,
                          BoardConfigurationRepository boardConfigurationRepository,
                          PlayerPurchaseHistoryRepository playerPurchaseHistoryRepository,
                          GameHistoryRepository gameHistoryRepository,
                          GameResultRepository gameResultRepository,
                          RPGPieceRepository rpgPieceRepository,
                          RPGModifierRepository rpgModifierRepository) {
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
        this.rpgPieceRepository = rpgPieceRepository;
        this.rpgModifierRepository = rpgModifierRepository;
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

        RPGRound firstRound = getRoundConfiguration(1);
        BoardConfiguration boardConfig = getBoardConfiguration(1, firstRound.getBoardSize());
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

        if (enemyConfig.getPieces() != null && !enemyConfig.getPieces().isEmpty()) {
            rpgGameState.getEnemyArmyConfig().setPieces(new ArrayList<>(enemyConfig.getPieces()));
        }

        RPGGameState savedState = rpgGameStateRepository.save(rpgGameState);
        updateGameSessionWithRpgState(gameSession, savedState);
        createGameHistory(gameSessionId, playerIds);

        logger.info("Created new RPG game for session: {}", gameSessionId);
        return savedState;
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
                gameMode != GameMode.MULTIPLAYER_RPG &&
                gameMode != GameMode.ENHANCED_RPG) {
            throw new IllegalArgumentException("Game session is not in RPG mode");
        }
    }

    private void validateMultiplayerSession(GameSession gameSession) {
        if (gameSession.getBlackPlayer() == null || gameSession.getBlackPlayer().isEmpty()) {
            throw new IllegalStateException("Multiplayer RPG requires at least one black player");
        }
    }

    private RPGRound getRoundConfiguration(int roundNumber) {
        return rpgRoundRepository.findByRoundNumber(String.valueOf(roundNumber))
                .orElseThrow(() -> new ConfigurationNotFoundException(
                        "Initial RPG round configuration not found for round: " + roundNumber));
    }

    private BoardConfiguration getBoardConfiguration(int round, int boardSize) {
        return boardConfigurationRepository.findByRoundAndBoardSize(round, boardSize)
                .orElseThrow(() -> new ConfigurationNotFoundException(
                        "Board configuration not found for round: " + round));
    }

    private EnemyArmyConfig getEnemyConfiguration(int round, int difficulty) {
        return enemyArmyConfigRepository.findByRoundAndDifficulty(round, difficulty)
                .orElseThrow(() -> new ConfigurationNotFoundException(
                        "Enemy army configuration not found for round: " + round));
    }

    private void initializePlayerLists(String userId, GameSession gameSession,
                                       List<String> playerIds, List<String> playerNames) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        playerIds.add(userId);
        playerNames.add(user.getFirstName() + " " + user.getLastName());

        if (gameSession.getGameMode() == GameMode.MULTIPLAYER_RPG) {
            gameSession.getBlackPlayer().stream()
                    .filter(PlayerSessionInfo::isConnected)
                    .forEach(player -> {
                        playerIds.add(player.getUserId());
                        playerNames.add(player.getDisplayName());
                    });
        }
    }

    private RPGGameState buildInitialGameState(String gameSessionId, GameMode gameMode,
                                               List<String> playerIds, List<String> playerNames,
                                               BoardConfiguration boardConfig, RPGRound firstRound,
                                               EnemyArmyConfig firstEnemyArmy) {
        return RPGGameState.builder()
                .gameId(UUID.randomUUID().toString())
                .gameSessionId(gameSessionId)
                .playerArmy(new ArrayList<>())
                .EnemyArmyConfig(firstEnemyArmy)  // Set the entire EnemyArmyConfig
                .activeModifiers(new ArrayList<>())
                .activeBoardModifiers(new ArrayList<>())
                .activeCapacityModifiers(new ArrayList<>())
                .boardEffects(boardConfig.getEffects() != null ?
                        new ArrayList<>(boardConfig.getEffects()) : new ArrayList<>())
                .boardSize(boardConfig.getBoardSize())
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

        int nextRound = gameState.getCurrentRound() + 1;
        RPGRound roundConfig = getRoundConfiguration(nextRound);
        BoardConfiguration boardConfig = getBoardConfiguration(nextRound, roundConfig.getBoardSize());
        EnemyArmyConfig enemyConfig = getEnemyConfiguration(nextRound, 1);

        updateGameStateForNextRound(gameState, nextRound, roundConfig, boardConfig, enemyConfig);

        if (gameState.getGameMode() == GameMode.MULTIPLAYER_RPG) {
            rotatePlayerTurn(gameState);
        }

        recordRoundProgressAction(gameState);

        return rpgGameStateRepository.save(gameState);
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
                session.getBlackPlayer().stream()
                        .anyMatch(p -> p.getUserId().equals(playerId) && p.isConnected());
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
                .gameEndReason(victory ? GameEndReason.CHECKMATE : GameEndReason.RESIGNATION)
                .build();
        gameResultRepository.save(gameResult);
    }

    private void endGameSession(RPGGameState gameState, boolean victory) {
        GameSession gameSession = gamesessionrepository.findById(gameState.getGameSessionId())
                .orElse(null);
        gameSessionService.endGame(
                gameState.getGameSessionId(),
                victory ? Objects.requireNonNull(gameSession).getCurrentPlayerId() : null
        );
    }

    @Override
    public List<RPGGameState> findActiveGamesByUser(String userId) {
        return rpgGameStateRepository.findActiveGamesByUserId(userId);
    }

    @Override
    public List<RPGGameState> findGamesByUser(String userId) {
        return rpgGameStateRepository.findByUserId(userId);
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
        }
        else if ("modifier".equals(shopItem.getType())) {
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
}