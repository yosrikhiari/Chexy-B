package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.Interactions.PlayerAction;
import org.example.chessmystic.Models.Mechanics.RPGGameState;
import org.example.chessmystic.Models.rpg.BoardEffect;
import org.example.chessmystic.Models.Transactions.RPGModifier;
import org.example.chessmystic.Models.rpg.RPGPiece;
import org.example.chessmystic.Service.interfaces.GameRelated.IPlayerActionService;
import org.example.chessmystic.Service.interfaces.GameRelated.IRPGGameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/rpg-game")
@CrossOrigin(origins = "http://localhost:4200")
public class RPGGameController {

    private final IRPGGameService rpgGameService;
    private final IPlayerActionService playerActionService;

    public RPGGameController(IRPGGameService rpgGameService, IPlayerActionService playerActionService) {
        this.rpgGameService = rpgGameService;
        this.playerActionService = playerActionService;
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
            RPGGameState gameState = rpgGameService.updateCoins(gameId, coinsToAdd, playerId);
            return ResponseEntity.ok(gameState);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
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

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> findGamesByUser(@PathVariable String userId) {
        try {
            List<RPGGameState> games = rpgGameService.findGamesByUser(userId);
            return ResponseEntity.ok(games);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to find user games", "message", e.getMessage()));
        }
    }

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
}