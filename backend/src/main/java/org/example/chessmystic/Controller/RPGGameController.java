package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Models.Interactions.PlayerAction;
import org.example.chessmystic.Models.Mechanics.RPGGameState;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Models.rpg.BoardEffect;
import org.example.chessmystic.Models.Transactions.RPGModifier;
import org.example.chessmystic.Models.Transactions.EquipmentItem;
import org.example.chessmystic.Models.rpg.RPGPiece;
import org.example.chessmystic.Models.rpg.SpecializationType;
import org.example.chessmystic.Models.rpg.AbilityId;
import org.example.chessmystic.Repository.GameSessionRepository;
import org.example.chessmystic.Service.interfaces.GameRelated.IPlayerActionService;
import org.example.chessmystic.Service.interfaces.GameRelated.IRPGGameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.example.chessmystic.Service.implementation.GameRelated.GameSessionService.logger;

@RestController
@RequestMapping("/rpg-game")
public class RPGGameController {

    private final IRPGGameService rpgGameService;
    private final IPlayerActionService playerActionService;
    private final GameSessionRepository gameSessionRepository;


    public RPGGameController(IRPGGameService rpgGameService, IPlayerActionService playerActionService, GameSessionRepository gameSessionRepository) {
        this.rpgGameService = rpgGameService;
        this.playerActionService = playerActionService;
        this.gameSessionRepository = gameSessionRepository;
    }

    @PostMapping
    public ResponseEntity<?> createRPGGame(@RequestParam String userId,
                                           @RequestParam String gameSessionId,
                                           @RequestParam(defaultValue = "false") boolean isMultiplayer) {
        try {
            RPGGameState gameState = rpgGameService.createRPGGame(userId, gameSessionId, isMultiplayer);
            return ResponseEntity.status(HttpStatus.CREATED).body(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create RPG game", "message", e.getMessage()));
        }
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<?> findById(@PathVariable String gameId) {
        try {
            Optional<RPGGameState> gameState = rpgGameService.findById(gameId);
            return gameState.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body((RPGGameState) Map.of("error", "RPG game not found")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to find RPG game", "message", e.getMessage()));
        }
    }

    @GetMapping("/session/{gameSessionId}")
    public ResponseEntity<?> findByGameSessionId(@PathVariable String gameSessionId) {
        try {
            RPGGameState gameState = rpgGameService.findByGameSessionId(gameSessionId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to find RPG game", "message", e.getMessage()));
        }
    }

    @PostMapping("/next-round/{gameId}")
    public ResponseEntity<?> progressToNextRound(@PathVariable String gameId) {
        try {
            RPGGameState gameState = rpgGameService.progressToNextRound(gameId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to progress to next round", "message", e.getMessage()));
        }
    }

    @PostMapping("/add-piece/{gameId}")
    public ResponseEntity<?> addPieceToArmy(@PathVariable String gameId,
                                            @RequestBody RPGPiece piece,
                                            @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.addPieceToArmy(gameId, piece, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add piece", "message", e.getMessage()));
        }
    }

    @PostMapping("/add-modifier/{gameId}")
    public ResponseEntity<?> addModifier(@PathVariable String gameId,
                                         @RequestBody RPGModifier modifier,
                                         @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.addModifier(gameId, modifier, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add modifier", "message", e.getMessage()));
        }
    }

    @PostMapping("/add-board-effect/{gameId}")
    public ResponseEntity<?> addBoardEffect(@PathVariable String gameId,
                                            @RequestBody BoardEffect effect,
                                            @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.addBoardEffect(gameId, effect, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add board effect", "message", e.getMessage()));
        }
    }

    @PatchMapping("/score/{gameId}")
    public ResponseEntity<?> updateScore(@PathVariable String gameId,
                                         @RequestParam int scoreToAdd,
                                         @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.updateScore(gameId, scoreToAdd, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update score", "message", e.getMessage()));
        }
    }

    @PatchMapping("/coins/{gameId}")
    public ResponseEntity<?> updateCoins(@PathVariable String gameId,
                                         @RequestParam int coinsToAdd,
                                         @RequestParam String playerId) {
        try {
            // Add debug logging
            logger.info("updateCoins called with gameId: {}, coinsToAdd: {}, playerId: {}",
                    gameId, coinsToAdd, playerId);

            // Validate inputs
            if (gameId == null || gameId.trim().isEmpty()) {
                logger.error("Invalid gameId: {}", gameId);
                return ResponseEntity.badRequest().body(Map.of("error", "Game ID cannot be null or empty"));
            }

            if (playerId == null || playerId.trim().isEmpty()) {
                logger.error("Invalid playerId: {}", playerId);
                return ResponseEntity.badRequest().body(Map.of("error", "Player ID cannot be null or empty"));
            }

            // Check if game exists
            Optional<RPGGameState> gameStateOpt = rpgGameService.findById(gameId);
            if (gameStateOpt.isEmpty()) {
                logger.error("Game not found: {}", gameId);
                return ResponseEntity.badRequest().body(Map.of("error", "Game not found: " + gameId));
            }

            RPGGameState existingState = gameStateOpt.get();
            logger.info("Found game state. Current coins: {}, isGameOver: {}",
                    existingState.getCoins(), existingState.isGameOver());

            // Check game session
            Optional<GameSession> sessionOpt = gameSessionRepository.findById(existingState.getGameSessionId());
            if (sessionOpt.isEmpty()) {
                logger.error("Game session not found: {}", existingState.getGameSessionId());
                return ResponseEntity.badRequest().body(Map.of("error", "Game session not found"));
            }

            GameSession session = sessionOpt.get();
            logger.info("Found game session. PlayerIds: {}, CurrentPlayerId: {}",
                    session.getPlayerIds(), session.getCurrentPlayerId());

            // Check if player is in the game
            if (!session.getPlayerIds().contains(playerId)) {
                logger.error("Player {} not in game. Game players: {}", playerId, session.getPlayerIds());
                return ResponseEntity.badRequest().body(Map.of("error", "Player not in this game: " + playerId));
            }

            // For single player RPG, skip turn validation
            if (existingState.getGameMode() != GameMode.SINGLE_PLAYER_RPG) {
                if (!session.getCurrentPlayerId().equals(playerId)) {
                    logger.error("Not player's turn. Current: {}, Requested: {}",
                            session.getCurrentPlayerId(), playerId);
                    return ResponseEntity.badRequest().body(Map.of("error", "Not your turn: " + playerId));
                }
            }

            RPGGameState gameState = rpgGameService.updateCoins(gameId, coinsToAdd, playerId);
            logger.info("Successfully updated coins. New total: {}", gameState.getCoins());
            return ResponseEntity.ok(gameState);

        } catch (IllegalStateException e) {
            logger.error("IllegalStateException in updateCoins: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage(), "type", "IllegalStateException"));
        } catch (IllegalArgumentException e) {
            logger.error("IllegalArgumentException in updateCoins: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage(), "type", "IllegalArgumentException"));
        } catch (RuntimeException e) {
            logger.error("RuntimeException in updateCoins: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage(), "type", e.getClass().getSimpleName()));
        } catch (Exception e) {
            logger.error("Unexpected error in updateCoins: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update coins", "message", e.getMessage()));
        }
    }

    @PostMapping("/end/{gameId}")
    public ResponseEntity<?> endGame(@PathVariable String gameId,
                                     @RequestParam boolean victory) {
        try {
            RPGGameState gameState = rpgGameService.endGame(gameId, victory);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to end game", "message", e.getMessage()));
        }
    }

    @GetMapping("/active/{userId}")
    public ResponseEntity<?> findActiveGamesByUser(@PathVariable String userId) {
        try {
            List<RPGGameState> games = rpgGameService.findActiveGamesByUser(userId);
            return ResponseEntity.ok(games);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to find active games", "message", e.getMessage()));
        }
    }

/*    @GetMapping("/user/{userId}")
    public ResponseEntity<?> findGamesByUser(@PathVariable String userId) {
        try {
            List<RPGGameState> games = rpgGameService.findGamesByUser(userId);
            return ResponseEntity.ok(games);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to find user games", "message", e.getMessage()));
        }
    }
*/
    @PostMapping("/purchase/{gameId}/{shopItemId}")
    public ResponseEntity<?> purchaseShopItem(@PathVariable String gameId,
                                              @PathVariable String shopItemId,
                                              @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.purchaseShopItem(gameId, shopItemId, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to purchase item", "message", e.getMessage()));
        }
    }

    // NEW: MVP endpoints
    @PostMapping("/specialization/{gameId}/{pieceId}")
    public ResponseEntity<?> chooseSpecialization(@PathVariable String gameId,
                                                  @PathVariable String pieceId,
                                                  @RequestParam SpecializationType specialization,
                                                  @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.chooseSpecialization(gameId, pieceId, specialization, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to choose specialization", "message", e.getMessage()));
        }
    }

    @PostMapping("/equip/{gameId}/{pieceId}")
    public ResponseEntity<?> equipItem(@PathVariable String gameId,
                                       @PathVariable String pieceId,
                                       @RequestBody EquipmentItem item,
                                       @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.equipItem(gameId, pieceId, item, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to equip item", "message", e.getMessage()));
        }
    }

    @PostMapping("/resolve-tie/{gameId}")
    public ResponseEntity<?> resolveTie(@PathVariable String gameId,
                                        @RequestParam String playerId,
                                        @RequestParam String choice) {
        try {
            RPGGameState gameState = rpgGameService.resolveTie(gameId, playerId, choice);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to resolve tie", "message", e.getMessage()));
        }
    }

    @PostMapping("/quests/spawn/{gameId}")
    public ResponseEntity<?> spawnQuests(@PathVariable String gameId,
                                         @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.spawnQuests(gameId, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to spawn quests", "message", e.getMessage()));
        }
    }

    @PostMapping("/quests/accept/{gameId}/{questId}")
    public ResponseEntity<?> acceptQuest(@PathVariable String gameId,
                                         @PathVariable String questId,
                                         @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.acceptQuest(gameId, questId, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to accept quest", "message", e.getMessage()));
        }
    }

    @PostMapping("/quests/complete/{gameId}/{questId}")
    public ResponseEntity<?> completeQuest(@PathVariable String gameId,
                                           @PathVariable String questId,
                                           @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.completeQuest(gameId, questId, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to complete quest", "message", e.getMessage()));
        }
    }

    @PostMapping("/xp/{gameId}/{pieceId}")
    public ResponseEntity<?> awardXp(@PathVariable String gameId,
                                     @PathVariable String pieceId,
                                     @RequestParam int xp,
                                     @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.awardXp(gameId, pieceId, xp, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to award XP", "message", e.getMessage()));
        }
    }

    @GetMapping("/actions/session/{gameSessionId}")
    public ResponseEntity<?> getActionsForGameSession(@PathVariable String gameSessionId) {
        try {
            List<PlayerAction> actions = playerActionService.getActionsForGameSession(gameSessionId);
            return ResponseEntity.ok(actions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve actions", "message", e.getMessage()));
        }
    }

    @GetMapping("/actions/player/{playerId}")
    public ResponseEntity<?> getActionsForPlayer(@PathVariable String playerId) {
        try {
            List<PlayerAction> actions = playerActionService.getActionsForPlayer(playerId);
            return ResponseEntity.ok(actions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve player actions", "message", e.getMessage()));
        }
    }

    @GetMapping("/actions/round/{gameSessionId}/{roundNumber}")
    public ResponseEntity<?> getActionsForGameAndRound(@PathVariable String gameSessionId,
                                                       @PathVariable int roundNumber) {
        try {
            List<PlayerAction> actions = playerActionService.getActionsForGameAndRound(gameSessionId, roundNumber);
            return ResponseEntity.ok(actions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve round actions", "message", e.getMessage()));
        }
    }
    @PostMapping("/ability/{gameId}/{pieceId}")
    public ResponseEntity<?> activateAbility(@PathVariable String gameId,
                                             @PathVariable String pieceId,
                                             @RequestParam AbilityId ability,
                                             @RequestParam(required = false) String targetPieceId,
                                             @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.activateAbility(gameId, pieceId, ability, targetPieceId, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to activate ability", "message", e.getMessage()));
        }
    }

    // --- NEW: Special mechanics endpoints ---

    @PostMapping("/dreamer/converse/{gameId}/{pieceId}")
    public ResponseEntity<?> converseWithDreamer(@PathVariable String gameId,
                                                 @PathVariable String pieceId,
                                                 @RequestParam String prompt,
                                                 @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.converseWithDreamer(gameId, pieceId, prompt, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to converse with Dreamer", "message", e.getMessage()));
        }
    }

    @PostMapping("/preacher/control/{gameId}/{preacherPieceId}/{targetEnemyPieceId}")
    public ResponseEntity<?> preacherControl(@PathVariable String gameId,
                                             @PathVariable String preacherPieceId,
                                             @PathVariable String targetEnemyPieceId,
                                             @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.preacherControl(gameId, preacherPieceId, targetEnemyPieceId, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to execute preacher control", "message", e.getMessage()));
        }
    }

    @PostMapping("/statue/trigger/{gameId}")
    public ResponseEntity<?> triggerStatue(@PathVariable String gameId,
                                           @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.triggerStatueEvent(gameId, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to trigger statue event", "message", e.getMessage()));
        }
    }

    @PostMapping("/music-cue/{gameId}")
    public ResponseEntity<?> setMusicCue(@PathVariable String gameId,
                                         @RequestParam String cueId,
                                         @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.setMusicCue(gameId, cueId, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to set music cue", "message", e.getMessage()));
        }
    }

    @PostMapping("/weaknesses/{gameId}/{pieceId}")
    public ResponseEntity<?> updateWeaknesses(@PathVariable String gameId,
                                              @PathVariable String pieceId,
                                              @RequestBody java.util.Set<org.example.chessmystic.Models.rpg.WeaknessType> weaknesses,
                                              @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.updateWeaknesses(gameId, pieceId, weaknesses, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update weaknesses", "message", e.getMessage()));
        }
    }

    @PostMapping("/kill/{gameId}/{killerPieceId}")
    public ResponseEntity<?> trackKill(@PathVariable String gameId,
                                       @PathVariable String killerPieceId,
                                       @RequestParam String playerId) {
        try {
            RPGGameState gameState = rpgGameService.trackKill(gameId, killerPieceId, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to track kill", "message", e.getMessage()));
        }
    }
}