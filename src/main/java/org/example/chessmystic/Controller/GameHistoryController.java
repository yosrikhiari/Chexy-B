package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.Tracking.GameResult;
import org.example.chessmystic.Models.Tracking.GameHistory;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Service.interfaces.GameRelated.IGameHistoryService;
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
@CrossOrigin(origins = "http://localhost:4200")
public class GameHistoryController {

    private final IGameHistoryService gameHistoryService;

    @Autowired
    public GameHistoryController(IGameHistoryService gameHistoryService) {
        this.gameHistoryService = gameHistoryService;
    }

    @PostMapping
    public ResponseEntity<?> createGameHistory(@RequestParam String gameSessionId) {
        try {
            // Note: Assumes a way to fetch GameSession, e.g., via a repository or service
            // You may need to inject a GameSessionRepository or service to fetch it
            GameSession session = new GameSession(); // Placeholder: Replace with actual retrieval
            session.setGameId(gameSessionId); // Set properties as needed
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
            GameHistory history = gameHistoryService.updateGameHistory(historyId, result, LocalDateTime.now());
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to complete game history", "message", "An unexpected error occurred"));
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
    public ResponseEntity<?> findByGameSessionId(@PathVariable String gameSessionId) {
        try {
            Optional<GameHistory> histories = gameHistoryService.findByGameSessionId(gameSessionId);
            return ResponseEntity.ok(histories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to find game histories", "message", "An unexpected error occurred"));
        }
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
}