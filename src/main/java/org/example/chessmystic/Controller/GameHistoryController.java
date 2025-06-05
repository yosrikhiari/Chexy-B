package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.Tracking.GameResult;
import org.example.chessmystic.Models.Tracking.GameHistory;
import org.example.chessmystic.Service.interfaces.GameRelated.IGameHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/game-history")
public class GameHistoryController {

    private final IGameHistoryService gameHistoryService;

    public GameHistoryController(IGameHistoryService gameHistoryService) {
        this.gameHistoryService = gameHistoryService;
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping
    public ResponseEntity<?> createGameHistory(@RequestParam String gameSessionId,
                                               @RequestParam List<String> userIds,
                                               @RequestParam(defaultValue = "false") boolean isRPGMode) {
        try {
            GameHistory history = gameHistoryService.createGameHistory(gameSessionId, userIds, isRPGMode);
            return ResponseEntity.status(HttpStatus.CREATED).body(history);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create game history", "message", "An unexpected error occurred"));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/complete/{historyId}")
    public ResponseEntity<?> completeGameHistory(@PathVariable String historyId, @RequestBody GameResult result) {
        try {
            GameHistory history = gameHistoryService.completeGameHistory(historyId, result);
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to complete game history", "message", "An unexpected error occurred"));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
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

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GameHistory>> findByUserId(@PathVariable String userId) {
        try {
            List<GameHistory> histories = gameHistoryService.findByUserId(userId);
            return ResponseEntity.ok(histories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/session/{gameSessionId}")
    public ResponseEntity<List<GameHistory>> findByGameSessionId(@PathVariable String gameSessionId) {
        try {
            List<GameHistory> histories = gameHistoryService.findByGameSessionId(gameSessionId);
            return ResponseEntity.ok(histories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/ranked/{userId}")
    public ResponseEntity<List<GameHistory>> findRankedGamesByUser(@PathVariable String userId) {
        try {
            List<GameHistory> histories = gameHistoryService.findRankedGamesByUser(userId);
            return ResponseEntity.ok(histories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/rpg/{userId}")
    public ResponseEntity<List<GameHistory>> findRPGGamesByUser(@PathVariable String userId) {
        try {
            List<GameHistory> histories = gameHistoryService.findRPGGamesByUser(userId);
            return ResponseEntity.ok(histories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/recent")
    public ResponseEntity<List<GameHistory>> findRecentGames(@RequestParam String since) {
        try {
            LocalDateTime sinceTime = LocalDateTime.parse(since);
            List<GameHistory> histories = gameHistoryService.findRecentGames(sinceTime);
            return ResponseEntity.ok(histories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
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