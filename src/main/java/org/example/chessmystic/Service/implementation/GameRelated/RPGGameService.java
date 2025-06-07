package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.GameStateandFlow.GameEndReason;
import org.example.chessmystic.Models.Interactions.ActionType;
import org.example.chessmystic.Models.Mechanics.RPGGameState;
import org.example.chessmystic.Models.Tracking.GameHistory;
import org.example.chessmystic.Models.Tracking.GameResult;
import org.example.chessmystic.Models.rpg.ArmyCapacity;
import org.example.chessmystic.Models.rpg.RPGPiece;
import org.example.chessmystic.Models.Transactions.RPGModifier;
import org.example.chessmystic.Models.rpg.BoardEffect;
import org.example.chessmystic.Models.Transactions.ShopItem;
import org.example.chessmystic.Models.GameStateandFlow.GameStatus;
import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Models.Tracking.PlayerSessionInfo;
import org.example.chessmystic.Models.Transactions.PlayerPurchaseHistory;
import org.example.chessmystic.Repository.RPGGameStateRepository;
import org.example.chessmystic.Repository.ShopItemRepository;
import org.example.chessmystic.Repository.RPGRoundRepository;
import org.example.chessmystic.Repository.EnemyArmyConfigRepository;
import org.example.chessmystic.Repository.BoardConfigurationRepository;
import org.example.chessmystic.Repository.PlayerPurchaseHistoryRepository;
import org.example.chessmystic.Repository.GameHistoryRepository;
import org.example.chessmystic.Repository.GameResultRepository;
import org.example.chessmystic.Service.implementation.UserService;
import org.example.chessmystic.Service.interfaces.GameRelated.IPlayerActionService;
import org.example.chessmystic.Service.interfaces.GameRelated.IRPGGameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RPGGameService implements IRPGGameService {

    private final RPGGameStateRepository rpgGameStateRepository;
    private final UserService userService;
    private final GameSessionService gameSessionService;
    private final ShopItemRepository shopItemRepository;
    private final IPlayerActionService playerActionService;
    private final RPGRoundRepository rpgRoundRepository;
    private final EnemyArmyConfigRepository enemyArmyConfigRepository;
    private final BoardConfigurationRepository boardConfigurationRepository;
    private final PlayerPurchaseHistoryRepository playerPurchaseHistoryRepository;
    private final GameHistoryRepository gameHistoryRepository;
    private final GameResultRepository gameResultRepository;

    @Autowired
    public RPGGameService(RPGGameStateRepository rpgGameStateRepository, UserService userService,
                          GameSessionService gameSessionService, ShopItemRepository shopItemRepository,
                          IPlayerActionService playerActionService, RPGRoundRepository rpgRoundRepository,
                          EnemyArmyConfigRepository enemyArmyConfigRepository,
                          BoardConfigurationRepository boardConfigurationRepository,
                          PlayerPurchaseHistoryRepository playerPurchaseHistoryRepository,
                          GameHistoryRepository gameHistoryRepository,
                          GameResultRepository gameResultRepository) {
        this.rpgGameStateRepository = rpgGameStateRepository;
        this.userService = userService;
        this.gameSessionService = gameSessionService;
        this.shopItemRepository = shopItemRepository;
        this.playerActionService = playerActionService;
        this.rpgRoundRepository = rpgRoundRepository;
        this.enemyArmyConfigRepository = enemyArmyConfigRepository;
        this.boardConfigurationRepository = boardConfigurationRepository;
        this.playerPurchaseHistoryRepository = playerPurchaseHistoryRepository;
        this.gameHistoryRepository = gameHistoryRepository;
        this.gameResultRepository = gameResultRepository;
    }

    @Override
    @Transactional
    public RPGGameState createRPGGame(String userId, String gameSessionId, boolean isMultiplayer) {
        var user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var gameSession = gameSessionService.findById(gameSessionId)
                .orElseThrow(() -> new RuntimeException("Game session not found"));

        if (!gameSession.getGameMode().equals(GameMode.SINGLE_PLAYER_RPG) &&
                !gameSession.getGameMode().equals(GameMode.MULTIPLAYER_RPG) &&
                !gameSession.getGameMode().equals(GameMode.ENHANCED_RPG)) {
            throw new IllegalArgumentException("Game session is not in RPG mode");
        }

        if (isMultiplayer && gameSession.getGameMode().equals(GameMode.SINGLE_PLAYER_RPG)) {
            throw new RuntimeException("Multiplayer RPG cannot be started in single-player mode");
        }

        if (isMultiplayer && (gameSession.getBlackPlayer() == null || gameSession.getBlackPlayer().isEmpty())) {
            throw new RuntimeException("Multiplayer RPG requires at least one black player");
        }

        var firstRound = rpgRoundRepository.findByRoundNumber("1")
                .orElseThrow(() -> new RuntimeException("Initial RPG round configuration not found"));

        var boardConfig = boardConfigurationRepository.findByRoundAndBoardSize(1, firstRound.getBoardSize())
                .orElseThrow(() -> new RuntimeException("Board configuration not found for round 1"));

        var enemyConfig = enemyArmyConfigRepository.findByRoundAndDifficulty(1, 1)
                .orElseThrow(() -> new RuntimeException("Enemy army configuration not found for round 1"));

        RPGGameState rpgGameState = RPGGameState.builder()
                .gameId(UUID.randomUUID().toString())
                .gameSessionId(gameSessionId)
                .playerArmy(new ArrayList<>())
                .enemyArmy(new ArrayList<>())
                .activeModifiers(new ArrayList<>())
                .activeBoardModifiers(new ArrayList<>())
                .activeCapacityModifiers(new ArrayList<>())
                .boardEffects(boardConfig.getEffects() != null ? new ArrayList<>(boardConfig.getEffects()) : new ArrayList<>())
                .boardSize(boardConfig.getBoardSize())
                .lives(3)
                .score(0)
                .coins(100)
                .isGameOver(false)
                .gameMode(isMultiplayer ? GameMode.MULTIPLAYER_RPG : GameMode.SINGLE_PLAYER_RPG)
                .status(GameStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .lastPlayerActivity(LocalDateTime.now())
                .currentRound(1)
                .currentRoundConfigId(firstRound.getRoundNumber())
                .actionHistoryIds(new ArrayList<>())
                .currentObjective(firstRound.getObjective().toString())
                .turnsRemaining(firstRound.getTurnLimit())
                .playerIds(new ArrayList<>())
                .playerNames(new ArrayList<>())
                .currentPlayerId(userId)
                .build();

        List<String> playerIds = new ArrayList<>();
        List<String> playerNames = new ArrayList<>();
        playerIds.add(userId);
        playerNames.add(user.getFirstName() + " " + user.getLastName());
        if (isMultiplayer) {
            for (PlayerSessionInfo blackPlayer : gameSession.getBlackPlayer()) {
                if (blackPlayer.isConnected()) {
                    playerIds.add(blackPlayer.getUserId());
                    playerNames.add(blackPlayer.getDisplayName());
                }
            }
        }
        rpgGameState.setPlayerIds(playerIds);
        rpgGameState.setPlayerNames(playerNames);

        if (enemyConfig.getPieces() != null && !enemyConfig.getPieces().isEmpty()) {
            for (RPGPiece piece : enemyConfig.getPieces()) {
                rpgGameState.getEnemyArmy().add(piece);
            }
        }

        RPGGameState savedState = rpgGameStateRepository.save(rpgGameState);
        gameSession.setRpgGameStateId(savedState.getGameId());
        gameSessionService.updateGameStatus(gameSessionId, GameStatus.ACTIVE);

        var gameHistory = GameHistory.builder()
                .gameSessionId(gameSessionId)
                .userIds(playerIds)
                .isRPGMode(true)
                .startTime(LocalDateTime.now())
                .build();
        gameHistoryRepository.save(gameHistory);

        return savedState;
    }

    @Override
    public Optional<RPGGameState> findById(String gameId) {
        return rpgGameStateRepository.findById(gameId);
    }

    @Override
    public RPGGameState findByGameSessionId(String gameSessionId) {
        return rpgGameStateRepository.findByGameSessionId(gameSessionId)
                .orElseThrow(() -> new RuntimeException("RPG game state not found for session: " + gameSessionId));
    }

    @Override
    @Transactional
    public RPGGameState progressToNextRound(String gameId) {
        RPGGameState gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("RPG game state not found"));

        if (gameState.isGameOver()) {
            throw new RuntimeException("Game is already over");
        }

        int nextRound = gameState.getCurrentRound() + 1;
        var roundConfig = rpgRoundRepository.findByRoundNumber(String.valueOf(nextRound))
                .orElseThrow(() -> new RuntimeException("Round configuration not found for round: " + nextRound));

        var boardConfig = boardConfigurationRepository.findByRoundAndBoardSize(nextRound, roundConfig.getBoardSize())
                .orElseThrow(() -> new RuntimeException("Board configuration not found for round: " + nextRound));

        var enemyConfig = enemyArmyConfigRepository.findByRoundAndDifficulty(nextRound, 1)
                .orElseThrow(() -> new RuntimeException("Enemy army configuration not found for round: " + nextRound));

        gameState.setCurrentRound(nextRound);
        gameState.setLastUpdated(LocalDateTime.now());
        gameState.setCurrentRoundConfigId(roundConfig.getRoundNumber());
        gameState.setCurrentObjective(roundConfig.getObjective().toString());
        gameState.setTurnsRemaining(roundConfig.getTurnLimit());
        gameState.setBoardSize(boardConfig.getBoardSize());
        gameState.setBoardEffects(boardConfig.getEffects() != null ? new ArrayList<>(boardConfig.getEffects()) : new ArrayList<>());
        if (gameState.getCompletedRounds() == null) {
            gameState.setCompletedRounds(new ArrayList<>());
        }
        gameState.getCompletedRounds().add(gameState.getCurrentRound() - 1);

        gameState.getEnemyArmy().clear();
        if (enemyConfig.getPieces() != null) {
            for (RPGPiece piece : enemyConfig.getPieces()) {
                gameState.getEnemyArmy().add(piece);
            }
        }

        if (gameState.getGameMode().equals(GameMode.MULTIPLAYER_RPG)) {
            List<String> playerIds = gameState.getPlayerIds().stream()
                    .filter(id -> gameSessionService.findById(gameState.getGameSessionId())
                            .map(session -> session.getPlayerIds().contains(id) &&
                                    (session.getWhitePlayer().getUserId().equals(id) ||
                                            session.getBlackPlayer().stream()
                                                    .anyMatch(p -> p.getUserId().equals(id) && p.isConnected())))
                            .orElse(false))
                    .collect(Collectors.toList());
            int currentIndex = playerIds.indexOf(gameState.getCurrentPlayerId());
            int nextIndex = (currentIndex + 1) % playerIds.size();
            gameState.setCurrentPlayerId(playerIds.get(nextIndex));
        }

        var gameSession = gameSessionService.findById(gameState.getGameSessionId())
                .orElseThrow(() -> new RuntimeException("Game session not found"));
        String playerId = gameSession.getWhitePlayer().getUserId();
        playerActionService.recordAction(
                gameState.getGameSessionId(), playerId, ActionType.NORMAL,
                -1, -1, -1, -1, null, gameState.getGameId(), gameState.getCurrentRound(),
                "ProgressToRound:" + nextRound, 0, false);

        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState addPieceToArmy(String gameId, RPGPiece piece, String playerId) {
        RPGGameState gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("RPG game state not found"));

        if (gameState.isGameOver()) {
            throw new RuntimeException("Cannot add piece to army in a completed game");
        }

        if (!gameState.getPlayerIds().contains(playerId)) {
            throw new RuntimeException("Player " + playerId + " is not part of this game");
        }

        if (!gameState.getCurrentPlayerId().equals(playerId)) {
            throw new RuntimeException("Not your turn");
        }

        if (gameState.getArmyCapacity() != null) {
            validateArmyCapacity(gameState.getArmyCapacity(), piece, gameState.getPlayerArmy());
        }

        if (gameState.getPlayerArmy() == null) {
            gameState.setPlayerArmy(new ArrayList<>());
        }
        gameState.getPlayerArmy().add(piece);
        gameState.setLastUpdated(LocalDateTime.now());

        if (gameState.getGameMode().equals(GameMode.MULTIPLAYER_RPG)) {
            List<String> playerIds = gameState.getPlayerIds().stream()
                    .filter(id -> gameSessionService.findById(gameState.getGameSessionId())
                            .map(session -> session.getPlayerIds().contains(id) &&
                                    (session.getWhitePlayer().getUserId().equals(id) ||
                                            session.getBlackPlayer().stream()
                                                    .anyMatch(p -> p.getUserId().equals(id) && p.isConnected())))
                            .orElse(false))
                    .collect(Collectors.toList());
            int currentIndex = playerIds.indexOf(gameState.getCurrentPlayerId());
            int nextIndex = (currentIndex + 1) % playerIds.size();
            gameState.setCurrentPlayerId(playerIds.get(nextIndex));
        }

        playerActionService.recordAction(
                gameState.getGameSessionId(), playerId, ActionType.NORMAL,
                -1, -1, 0, 0, null, gameState.getGameId(), gameState.getCurrentRound(),
                "AddPiece:" + piece.getType(), 0, false);

        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState addModifier(String gameId, RPGModifier modifier, String playerId) {
        RPGGameState gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("RPG game state not found"));

        if (gameState.isGameOver()) {
            throw new RuntimeException("Cannot add modifier to a completed game");
        }

        if (!gameState.getPlayerIds().contains(playerId)) {
            throw new RuntimeException("Player " + playerId + " is not part of this game");
        }

        if (!gameState.getCurrentPlayerId().equals(playerId)) {
            throw new RuntimeException("Not your turn");
        }

        if (gameState.getActiveModifiers() == null) {
            gameState.setActiveModifiers(new ArrayList<>());
        }
        gameState.getActiveModifiers().add(modifier);
        gameState.setLastUpdated(LocalDateTime.now());

        if (gameState.getGameMode().equals(GameMode.MULTIPLAYER_RPG)) {
            List<String> playerIds = gameState.getPlayerIds().stream()
                    .filter(id -> gameSessionService.findById(gameState.getGameSessionId())
                            .map(session -> session.getPlayerIds().contains(id) &&
                                    (session.getWhitePlayer().getUserId().equals(id) ||
                                            session.getBlackPlayer().stream()
                                                    .anyMatch(p -> p.getUserId().equals(id) && p.isConnected())))
                            .orElse(false))
                    .collect(Collectors.toList());
            int currentIndex = playerIds.indexOf(gameState.getCurrentPlayerId());
            int nextIndex = (currentIndex + 1) % playerIds.size();
            gameState.setCurrentPlayerId(playerIds.get(nextIndex));
        }

        playerActionService.recordAction(
                gameState.getGameSessionId(), playerId, ActionType.NORMAL,
                -1, -1, -1, -1, null, gameState.getGameId(), gameState.getCurrentRound(),
                "AddModifier:" + modifier.getName(), 0, false);

        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState addBoardEffect(String gameId, BoardEffect effect, String playerId) {
        RPGGameState gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("RPG game state not found"));

        if (gameState.isGameOver()) {
            throw new RuntimeException("Cannot add board effect to a completed game");
        }

        if (!gameState.getPlayerIds().contains(playerId)) {
            throw new RuntimeException("Player " + playerId + " is not part of this game");
        }

        if (!gameState.getCurrentPlayerId().equals(playerId)) {
            throw new RuntimeException("Not your turn");
        }

        if (gameState.getBoardEffects() == null) {
            gameState.setBoardEffects(new ArrayList<>());
        }
        gameState.getBoardEffects().add(effect);
        gameState.setLastUpdated(LocalDateTime.now());

        if (gameState.getGameMode().equals(GameMode.MULTIPLAYER_RPG)) {
            List<String> playerIds = gameState.getPlayerIds().stream()
                    .filter(id -> gameSessionService.findById(gameState.getGameSessionId())
                            .map(session -> session.getPlayerIds().contains(id) &&
                                    (session.getWhitePlayer().getUserId().equals(id) ||
                                            session.getBlackPlayer().stream()
                                                    .anyMatch(p -> p.getUserId().equals(id) && p.isConnected())))
                            .orElse(false))
                    .collect(Collectors.toList());
            int currentIndex = playerIds.indexOf(gameState.getCurrentPlayerId());
            int nextIndex = (currentIndex + 1) % playerIds.size();
            gameState.setCurrentPlayerId(playerIds.get(nextIndex));
        }

        playerActionService.recordAction(
                gameState.getGameSessionId(), playerId, ActionType.NORMAL,
                -1, -1, -1, -1, null, gameState.getGameId(), gameState.getCurrentRound(),
                "AddBoardEffect:" + effect.getName(), 0, false);

        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState updateScore(String gameId, int scoreToAdd, String playerId) {
        RPGGameState gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("RPG game state not found"));

        if (gameState.isGameOver()) {
            throw new RuntimeException("Cannot update score in a completed game");
        }

        if (!gameState.getPlayerIds().contains(playerId)) {
            throw new RuntimeException("Player " + playerId + " is not part of this game");
        }

        if (!gameState.getCurrentPlayerId().equals(playerId)) {
            throw new RuntimeException("Not your turn");
        }

        gameState.setScore(gameState.getScore() + scoreToAdd);
        gameState.setLastUpdated(LocalDateTime.now());

        if (gameState.getGameMode().equals(GameMode.MULTIPLAYER_RPG)) {
            List<String> playerIds = gameState.getPlayerIds().stream()
                    .filter(id -> gameSessionService.findById(gameState.getGameSessionId())
                            .map(session -> session.getPlayerIds().contains(id) &&
                                    (session.getWhitePlayer().getUserId().equals(id) ||
                                            session.getBlackPlayer().stream()
                                                    .anyMatch(p -> p.getUserId().equals(id) && p.isConnected())))
                            .orElse(false))
                    .collect(Collectors.toList());
            int currentIndex = playerIds.indexOf(gameState.getCurrentPlayerId());
            int nextIndex = (currentIndex + 1) % playerIds.size();
            gameState.setCurrentPlayerId(playerIds.get(nextIndex));
        }

        playerActionService.recordAction(
                gameState.getGameSessionId(), playerId, ActionType.NORMAL,
                -1, -1, -1, -1, null, gameState.getGameId(), gameState.getCurrentRound(),
                "UpdateScore:" + scoreToAdd, 0, false);

        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState updateCoins(String gameId, int coinsToAdd, String playerId) {
        RPGGameState gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("RPG game state not found"));

        if (gameState.isGameOver()) {
            throw new RuntimeException("Cannot update coins in a completed game");
        }

        if (!gameState.getPlayerIds().contains(playerId)) {
            throw new RuntimeException("Player " + playerId + " is not part of this game");
        }

        if (!gameState.getCurrentPlayerId().equals(playerId)) {
            throw new RuntimeException("Not your turn");
        }

        gameState.setCoins(gameState.getCoins() + coinsToAdd);
        gameState.setLastUpdated(LocalDateTime.now());

        if (gameState.getGameMode().equals(GameMode.MULTIPLAYER_RPG)) {
            List<String> playerIds = gameState.getPlayerIds().stream()
                    .filter(id -> gameSessionService.findById(gameState.getGameSessionId())
                            .map(session -> session.getPlayerIds().contains(id) &&
                                    (session.getWhitePlayer().getUserId().equals(id) ||
                                            session.getBlackPlayer().stream()
                                                    .anyMatch(p -> p.getUserId().equals(id) && p.isConnected())))
                            .orElse(false))
                    .collect(Collectors.toList());
            int currentIndex = playerIds.indexOf(gameState.getCurrentPlayerId());
            int nextIndex = (currentIndex + 1) % playerIds.size();
            gameState.setCurrentPlayerId(playerIds.get(nextIndex));
        }

        playerActionService.recordAction(
                gameState.getGameSessionId(), playerId, ActionType.NORMAL,
                -1, -1, -1, -1, null, gameState.getGameId(), gameState.getCurrentRound(),
                "UpdateCoins:" + coinsToAdd, 0, false);

        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState endGame(String gameId, boolean victory) {
        RPGGameState gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("RPG game state not found"));

        gameState.setGameOver(true);
        gameState.setStatus(victory ? GameStatus.COMPLETED : GameStatus.ABANDONED);
        gameState.setLastUpdated(LocalDateTime.now());

        var gameSession = gameSessionService.findById(gameState.getGameSessionId())
                .orElseThrow(() -> new RuntimeException("Game session not found"));

        playerActionService.recordAction(
                gameState.getGameSessionId(), gameState.getCurrentPlayerId(), ActionType.NORMAL,
                -1, -1, -1, -1, null, gameState.getGameId(), gameState.getCurrentRound(),
                "EndGame:" + (victory ? "Victory" : "Abandoned"), 0, false);

        var gameHistory = gameHistoryRepository.findByGameSessionId(gameState.getGameSessionId())
                .orElseThrow(() -> new RuntimeException("Game history not found"));
        gameHistory.setEndTime(LocalDateTime.now());
        gameHistory.setIsCompleted(victory);
        gameHistory.setFinalRound(gameState.getCurrentRound());
        gameHistory.setFinalScore(gameState.getScore());
        gameHistoryRepository.save(gameHistory);

        var gameResult = GameResult.builder()
                .gameSessionId(gameState.getGameSessionId())
                .winnerId(victory ? gameState.getPlayerIds().get(0) : null)
                .score(gameState.getScore())
                .gameEndReason(victory ? GameEndReason.CHECKMATE : GameEndReason.RESIGNATION)
                .build();
        gameResultRepository.save(gameResult);

        gameSessionService.updateGameStatus(gameState.getGameSessionId(), gameState.getStatus());

        return rpgGameStateRepository.save(gameState);
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
        RPGGameState gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("RPG game state not found"));

        ShopItem shopItem = shopItemRepository.findById(shopItemId)
                .orElseThrow(() -> new RuntimeException("Shop item not found"));

        if (gameState.getCoins() < shopItem.getCost()) {
            throw new RuntimeException("Insufficient coins to purchase item");
        }

        if (!gameState.getPlayerIds().contains(playerId)) {
            throw new RuntimeException("Player " + playerId + " is not part of this game");
        }

        if (!gameState.getCurrentPlayerId().equals(playerId)) {
            throw new RuntimeException("Not your turn");
        }

        if ("piece".equals(shopItem.getType()) && shopItem.getItem() instanceof RPGPiece) {
            if (gameState.getArmyCapacity() != null) {
                validateArmyCapacity(gameState.getArmyCapacity(), (RPGPiece) shopItem.getItem(), gameState.getPlayerArmy());
            }
            if (gameState.getPlayerArmy() == null) {
                gameState.setPlayerArmy(new ArrayList<>());
            }
            gameState.getPlayerArmy().add((RPGPiece) shopItem.getItem());
        } else if ("modifier".equals(shopItem.getType()) && shopItem.getItem() instanceof RPGModifier) {
            if (gameState.getActiveModifiers() == null) {
                gameState.setActiveModifiers(new ArrayList<>());
            }
            gameState.getActiveModifiers().add((RPGModifier) shopItem.getItem());
        } else {
            throw new RuntimeException("Invalid shop item type");
        }

        gameState.setCoins(gameState.getCoins() - shopItem.getCost());

        var purchaseHistory = PlayerPurchaseHistory.builder()
                .userId(playerId)
                .gameSessionId(gameState.getGameSessionId())
                .shopItems(List.of(shopItem))
                .costPaid(shopItem.getCost())
                .purchaseTime(LocalDateTime.now())
                .build();
        playerPurchaseHistoryRepository.save(purchaseHistory);

        if (shopItem.getOwnedPieceIds() == null) {
            shopItem.setOwnedPieceIds(new ArrayList<>());
        }
        if (shopItem.getPurchasedModifierIds() == null) {
            shopItem.setPurchasedModifierIds(new ArrayList<>());
        }
        if (shopItem.getItem() instanceof RPGPiece) {
            shopItem.getOwnedPieceIds().add(((RPGPiece) shopItem.getItem()).getId());
        } else if (shopItem.getItem() instanceof RPGModifier) {
            shopItem.getPurchasedModifierIds().add(((RPGModifier) shopItem.getItem()).getId());
        }

        shopItemRepository.save(shopItem);
        gameState.setLastUpdated(LocalDateTime.now());

        if (gameState.getGameMode().equals(GameMode.MULTIPLAYER_RPG)) {
            List<String> playerIds = gameState.getPlayerIds().stream()
                    .filter(id -> gameSessionService.findById(gameState.getGameSessionId())
                            .map(session -> session.getPlayerIds().contains(id) &&
                                    (session.getWhitePlayer().getUserId().equals(id) ||
                                            session.getBlackPlayer().stream()
                                                    .anyMatch(p -> p.getUserId().equals(id) && p.isConnected())))
                            .orElse(false))
                    .collect(Collectors.toList());
            int currentIndex = playerIds.indexOf(gameState.getCurrentPlayerId());
            int nextIndex = (currentIndex + 1) % playerIds.size();
            gameState.setCurrentPlayerId(playerIds.get(nextIndex));
        }

        playerActionService.recordAction(
                gameState.getGameSessionId(), playerId, ActionType.NORMAL,
                -1, -1, -1, -1, null, gameState.getGameId(), gameState.getCurrentRound(),
                "Purchase:" + shopItem.getName(), shopItem.getCost(), false);

        return rpgGameStateRepository.save(gameState);
    }

    private void validateArmyCapacity(ArmyCapacity cap, RPGPiece piece, List<RPGPiece> army) {
        long count = army != null ? army.stream().filter(p -> p.getType() == piece.getType()).count() : 0;
        switch (piece.getType()) {
            case QUEEN:
                if (count >= cap.getMaxQueens()) throw new RuntimeException("Max queens limit exceeded");
                break;
            case ROOK:
                if (count >= cap.getMaxRooks()) throw new RuntimeException("Max rooks limit exceeded");
                break;
            case BISHOP:
                if (count >= cap.getMaxBishops()) throw new RuntimeException("Max bishops limit exceeded");
                break;
            case KNIGHT:
                if (count >= cap.getMaxKnights()) throw new RuntimeException("Max knights limit exceeded");
                break;
            case PAWN:
                if (count >= cap.getMaxPawns()) throw new RuntimeException("Max pawns limit exceeded");
                break;
            default:
                break;
        }
        if (army != null && army.size() >= cap.getMaxTotalPieces()) {
            throw new RuntimeException("Max total army capacity exceeded");
        }
    }
}