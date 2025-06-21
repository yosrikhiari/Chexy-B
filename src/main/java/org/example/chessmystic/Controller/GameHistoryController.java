package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.Interactions.PlayerAction;
import org.example.chessmystic.Models.Tracking.GameResult;
import org.example.chessmystic.Models.Tracking.GameHistory;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Service.interfaces.GameRelated.IGameHistoryService;
import org.example.chessmystic.Service.interfaces.GameRelated.IGameSessionService;
import org.example.chessmystic.Service.interfaces.GameRelated.IPlayerActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/game-history")
public class GameHistoryController {

    private final IGameHistoryService gameHistoryService;
    private final IGameSessionService gameSessionService;
    private final IPlayerActionService playerActionService;


    @Autowired
    public GameHistoryController(IGameHistoryService gameHistoryService, IGameSessionService gameSessionService, IPlayerActionService playerActionService) {
        this.gameHistoryService = gameHistoryService;
        this.gameSessionService = gameSessionService;
        this.playerActionService = playerActionService;
    }

    @PostMapping
    public ResponseEntity<?> createGameHistory(@RequestParam String gameSessionId) {
        try {
            GameSession session = gameSessionService.findById(gameSessionId)
                    .orElseThrow(() -> new RuntimeException("Game session not found"));
            GameHistory history = gameHistoryService.createGameHistory(session);
            return ResponseEntity.status(HttpStatus.CREATED).body(history);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create game history", "message", "An unexpected error occurred"));
        }
    }

    @PostMapping("/complete/{historyId}")
    public ResponseEntity<?> completeGameHistory(@PathVariable String historyId, @RequestBody GameResult result) {
        try {
            // Add validation for historyId
            if (historyId == null || historyId.trim().isEmpty() || "undefined".equals(historyId) || "null".equals(historyId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid history ID",
                                "message", "History ID cannot be null, empty, or undefined",
                                "received", historyId));
            }

            // Check if game history exists before trying to update
            Optional<GameHistory> existingHistory = gameHistoryService.findById(historyId);
            if (existingHistory.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Game history not found",
                                "message", "No game history found with ID: " + historyId));
            }

            // Validate the result object
            if (result == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid game result",
                                "message", "Game result cannot be null"));
            }

            GameHistory history = gameHistoryService.updateGameHistory(historyId, result, LocalDateTime.now());
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid history ID or data",
                            "message", e.getMessage(),
                            "historyId", historyId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to complete game history",
                            "message", "An unexpected error occurred",
                            "historyId", historyId));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable String id) {
        try {
            Optional<GameHistory> history = gameHistoryService.findById(id);
            return history.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body((GameHistory) Map.of("error", "Game history not found")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to find game history", "message", "An unexpected error occurred"));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> findByUserId(@PathVariable String userId) {
        try {
            List<GameHistory> histories = gameHistoryService.findByUserId(userId);
            return ResponseEntity.ok(histories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to find game histories", "message", "An unexpected error occurred"));
        }
    }

    @GetMapping("/session/{gameSessionId}")
    public ResponseEntity<GameHistory> findByGameSessionId(@PathVariable String gameSessionId) {
        Optional<GameHistory> history = gameHistoryService.findByGameSessionId(gameSessionId);
        return history.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/ranked/{userId}")
    public ResponseEntity<?> findRankedGamesByUser(@PathVariable String userId) {
        try {
            List<GameHistory> histories = gameHistoryService.findRankedGamesByUser(userId);
            return ResponseEntity.ok(histories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to find ranked games", "message", "An unexpected error occurred"));
        }
    }

    @GetMapping("/rpg/{userId}")
    public ResponseEntity<?> findRPGGamesByUser(@PathVariable String userId) {
        try {
            List<GameHistory> histories = gameHistoryService.findRPGGamesByUser(userId);
            return ResponseEntity.ok(histories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to find RPG games", "message", "An unexpected error occurred"));
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<?> findRecentGames(@RequestParam String since) {
        try {
            LocalDateTime sinceTime = LocalDateTime.parse(since);
            List<GameHistory> histories = gameHistoryService.findRecentGames(sinceTime);
            return ResponseEntity.ok(histories);
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid date format", "message", "Use ISO-8601 format, e.g., 2025-06-07T19:54:00"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to find recent games", "message", "An unexpected error occurred"));
        }
    }

    @PostMapping("/action/{historyId}/{playerActionId}")
    public ResponseEntity<?> addPlayerAction(@PathVariable String historyId, @PathVariable String playerActionId) {
        try {
            GameHistory history = gameHistoryService.addPlayerAction(historyId, playerActionId);
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add player action", "message", "An unexpected error occurred"));
        }
    }


    @GetMapping("/session/{gameSessionId}/actions")
    public ResponseEntity<List<PlayerAction>> getActionsForGameSession(@PathVariable String gameSessionId) {
        List<PlayerAction> actions = playerActionService.getActionsForGameSession(gameSessionId);
        return ResponseEntity.ok(actions);
    }



    @PostMapping("/ensure/{gameSessionId}")
    public ResponseEntity<?> ensureGameHistory(@PathVariable String gameSessionId) {
        try {
            if (gameSessionId == null || gameSessionId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid game session ID"));
            }

            // Try to find existing history first
            Optional<GameHistory> existingHistory = gameHistoryService.findByGameSessionId(gameSessionId);
            if (existingHistory.isPresent()) {
                return ResponseEntity.ok(existingHistory.get());
            }

            // If not found, create new one
            GameSession session = gameSessionService.findById(gameSessionId)
                    .orElseThrow(() -> new RuntimeException("Game session not found"));
            GameHistory history = gameHistoryService.createGameHistory(session);
            return ResponseEntity.status(HttpStatus.CREATED).body(history);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to ensure game history",
                            "message", "An unexpected error occurred"));
        }
    }
}