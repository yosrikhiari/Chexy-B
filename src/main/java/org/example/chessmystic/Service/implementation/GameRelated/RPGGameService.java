package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.Interactions.ActionType;
import org.example.chessmystic.Models.Mechanics.RPGGameState;
import org.example.chessmystic.Models.rpg.RPGPiece;
import org.example.chessmystic.Models.Transactions.RPGModifier;
import org.example.chessmystic.Models.rpg.BoardEffect;
import org.example.chessmystic.Models.Transactions.ShopItem;
import org.example.chessmystic.Models.GameStateandFlow.GameStatus;
import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Repository.RPGGameStateRepository;
import org.example.chessmystic.Repository.ShopItemRepository;

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

@Service
public class RPGGameService implements IRPGGameService {

    private final RPGGameStateRepository rpgGameStateRepository;
    private final UserService userService;
    private final GameSessionService gameSessionService;
    private final ShopItemRepository shopItemRepository;
    private final IPlayerActionService playerActionService;

    @Autowired
    public RPGGameService(RPGGameStateRepository rpgGameStateRepository, UserService userService, GameSessionService gameSessionService, ShopItemRepository shopItemRepository, IPlayerActionService playerActionService) {
        this.rpgGameStateRepository = rpgGameStateRepository;
        this.userService = userService;
        this.gameSessionService = gameSessionService;
        this.shopItemRepository = shopItemRepository;
        this.playerActionService = playerActionService;
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

        if (isMultiplayer && gameSession.getBlackPlayer() == null) {
            throw new RuntimeException("Multiplayer RPG requires a second player");
        }

        RPGGameState rpgGameState = RPGGameState.builder()
                .gameId(gameSessionId)
                .userId(userId)
                .playerKeycloakId(user.getKeycloakId())
                .playerName(user.getFirstName() + " " + user.getLastName())
                .currentRound(1)
                .playerArmy(new ArrayList<>())
                .activeModifiers(new ArrayList<>())
                .activeBoardModifiers(new ArrayList<>())
                .activeCapacityModifiers(new ArrayList<>())
                .boardEffects(new ArrayList<>())
                .boardSize(8) // Standard chess board size
                .lives(3) // Default lives
                .score(0)
                .coins(100) // Starting coins
                .isGameOver(false)
                .gameMode(isMultiplayer ? GameMode.MULTIPLAYER_RPG : GameMode.SINGLE_PLAYER_RPG)
                .status(GameStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .lastPlayerActivity(LocalDateTime.now())
                .gameSessionId(gameSessionId)
                .build();

        if (isMultiplayer) {
            var opponent = gameSession.getBlackPlayer();
            rpgGameState.setOpponentId(opponent.getUserId());
            rpgGameState.setOpponentName(opponent.getDisplayName());
            rpgGameState.setPlayerTurn(true);
        }

        RPGGameState savedState = rpgGameStateRepository.save(rpgGameState);
        gameSession.setRpgGameStateId(savedState.getGameId());
        gameSessionService.updateGameStatus(gameSessionId, GameStatus.ACTIVE);
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

        gameState.setCurrentRound(gameState.getCurrentRound() + 1);
        gameState.setLastUpdated(LocalDateTime.now());
        if (gameState.getCompletedRounds() == null) {
            gameState.setCompletedRounds(new ArrayList<>());
        }
        gameState.getCompletedRounds().add(gameState.getCurrentRound() - 1);

        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState addPieceToArmy(String gameId, RPGPiece piece) {
        RPGGameState gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("RPG game state not found"));

        if (gameState.isGameOver()) {
            throw new RuntimeException("Cannot add piece to army in a completed game");
        }

        if (gameState.getPlayerArmy() == null) {
            gameState.setPlayerArmy(new ArrayList<>());
        }
        gameState.getPlayerArmy().add(piece);
        gameState.setLastUpdated(LocalDateTime.now());

        // Record action for adding piece (assuming initial placement at a position)
        playerActionService.recordAction(
                gameState.getGameSessionId(), gameState.getUserId(), ActionType.NORMAL,
                -1, -1, 0, 0, // Placeholder coordinates; adjust based on actual placement logic
                null, gameState.getGameId(), gameState.getCurrentRound(), "AddPiece:" + piece.getType(), 0, false);

        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState addModifier(String gameId, RPGModifier modifier) {
        RPGGameState gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("RPG game state not found"));

        if (gameState.isGameOver()) {
            throw new RuntimeException("Cannot add modifier to a completed game");
        }

        if (gameState.getActiveModifiers() == null) {
            gameState.setActiveModifiers(new ArrayList<>());
        }
        gameState.getActiveModifiers().add(modifier);
        gameState.setLastUpdated(LocalDateTime.now());

        // Record action for adding modifier
        playerActionService.recordAction(
                gameState.getGameSessionId(), gameState.getUserId(), ActionType.NORMAL,
                -1, -1, -1, -1, // No coordinates for modifier application
                null, gameState.getGameId(), gameState.getCurrentRound(), "AddModifier:" + modifier.getName(), 0, false);

        return rpgGameStateRepository.save(gameState);
    }


    @Override
    @Transactional
    public RPGGameState addBoardEffect(String gameId, BoardEffect effect) {
        RPGGameState gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("RPG game state not found"));

        if (gameState.isGameOver()) {
            throw new RuntimeException("Cannot add board effect to a completed game");
        }

        if (gameState.getBoardEffects() == null) {
            gameState.setBoardEffects(new ArrayList<>());
        }
        gameState.getBoardEffects().add(effect);
        gameState.setLastUpdated(LocalDateTime.now());

        // Record action for adding board effect
        playerActionService.recordAction(
                gameState.getGameSessionId(), gameState.getUserId(), ActionType.NORMAL,
                -1, -1, -1, -1, // No coordinates for board effect
                null, gameState.getGameId(), gameState.getCurrentRound(), "AddBoardEffect:" + effect.getName(), 0, false);

        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState updateScore(String gameId, int scoreToAdd) {
        RPGGameState gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("RPG game state not found"));

        if (gameState.isGameOver()) {
            throw new RuntimeException("Cannot update score in a completed game");
        }

        gameState.setScore(gameState.getScore() + scoreToAdd);
        gameState.setLastUpdated(LocalDateTime.now());

        return rpgGameStateRepository.save(gameState);
    }

    @Override
    @Transactional
    public RPGGameState updateCoins(String gameId, int coinsToAdd) {
        RPGGameState gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("RPG game state not found"));

        if (gameState.isGameOver()) {
            throw new RuntimeException("Cannot update coins in a completed game");
        }

        gameState.setCoins(gameState.getCoins() + coinsToAdd);
        gameState.setLastUpdated(LocalDateTime.now());

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
    public RPGGameState purchaseShopItem(String gameId, String shopItemId) {
        RPGGameState gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("RPG game state not found"));

        ShopItem shopItem = shopItemRepository.findById(shopItemId)
                .orElseThrow(() -> new RuntimeException("Shop item not found"));

        if (gameState.getCoins() < shopItem.getCost()) {
            throw new RuntimeException("Insufficient coins to purchase item");
        }

        gameState.setCoins(gameState.getCoins() - shopItem.getCost());

        if ("piece".equals(shopItem.getType()) && shopItem.getItem() instanceof RPGPiece) {
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

        return rpgGameStateRepository.save(gameState);
    }
}