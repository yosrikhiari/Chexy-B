package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Models.GameStateandFlow.GameState;
import org.example.chessmystic.Models.GameStateandFlow.GameStatus;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Models.UIUX.TieResolutionOption;
import org.example.chessmystic.Service.interfaces.GameRelated.IGameSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/game-session")
public class GameSessionController {

    private final IGameSessionService gameSessionService;


    public GameSessionController(IGameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    @PostMapping
    public ResponseEntity<?> createGameSession(@RequestParam String playerId,
                                               @RequestParam GameMode gameMode,
                                               @RequestParam(defaultValue = "false") boolean isPrivate,
                                               @RequestParam(required = false) String inviteCode) {
        try {
            GameSession session = gameSessionService.createGameSession(playerId, gameMode, isPrivate, inviteCode);
            return ResponseEntity.status(HttpStatus.CREATED).body(session);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create game session", "message", "An unexpected error occurred"));
        }
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<?> findById(@PathVariable String gameId) {
        try {
            Optional<GameSession> session = gameSessionService.findById(gameId);
            return session.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body((GameSession) Map.of("error", "Game session not found")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to find game session", "message", "An unexpected error occurred"));
        }
    }

    @GetMapping("/invite/{inviteCode}")
    public ResponseEntity<?> findByInviteCode(@PathVariable String inviteCode) {
        try {
            Optional<GameSession> session = gameSessionService.findByInviteCode(inviteCode);
            return session.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body((GameSession) Map.of("error", "Game session not found")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to find game session", "message", "An unexpected error occurred"));
        }
    }

    @PostMapping("/join/{gameId}")
    public ResponseEntity<?> joinGame(@PathVariable String gameId,
                                      @RequestParam String playerId,
                                      @RequestParam(required = false) String inviteCode) {
        try {
            GameSession session = gameSessionService.joinGame(gameId, playerId, inviteCode);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to join game", "message", "An unexpected error occurred"));
        }
    }

    @PostMapping("/start/{gameId}")
    public ResponseEntity<?> startGame(@PathVariable String gameId) {
        try {
            GameSession session = gameSessionService.startGame(gameId);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to start game", "message", "An unexpected error occurred"));
        }
    }

    @PostMapping("/end/{gameId}")
    public ResponseEntity<?> endGame(@PathVariable String gameId,
                                     @RequestParam(required = false) String winnerId,
                                     @RequestParam(required = false, defaultValue = "false") boolean isDraw,
                                     @RequestParam(required = false) TieResolutionOption tieOption) {
        try {
            GameSession session = gameSessionService.endGame(gameId, winnerId, isDraw, tieOption);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to end game", "message", "An unexpected error occurred"));
        }
    }

    @GetMapping("/active/{playerId}")
    public ResponseEntity<List<GameSession>> findActiveGamesForPlayer(@PathVariable String playerId) {
        try {
            List<GameSession> sessions = gameSessionService.findActiveGamesForPlayer(playerId);
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @GetMapping("/available")
    public ResponseEntity<List<GameSession>> findAvailableGames() {
        try {
            List<GameSession> sessions = gameSessionService.findAvailableGames();
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @PatchMapping("/status/{gameId}")
    public ResponseEntity<?> updateGameStatus(@PathVariable String gameId, @RequestParam GameStatus status) {
        try {
            GameSession session = gameSessionService.updateGameStatus(gameId, status);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update game status", "message", "An unexpected error occurred"));
        }
    }

    @PatchMapping("/last-seen/{gameId}/{playerId}")
    public ResponseEntity<?> updatePlayerLastSeen(@PathVariable String gameId, @PathVariable String playerId) {
        try {
            gameSessionService.updatePlayerLastSeen(gameId, playerId);
            return ResponseEntity.ok(Map.of("message", "Player last seen updated"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update last seen", "message", "An unexpected error occurred"));
        }
    }

    @PostMapping("/reconnect/{gameId}/{playerId}")
    public ResponseEntity<?> reconnectPlayer(@PathVariable String gameId, @PathVariable String playerId) {
        try {
            GameSession session = gameSessionService.reconnectPlayer(gameId, playerId);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reconnect player", "message", "An unexpected error occurred"));
        }
    }

    @GetMapping("/mode/{gameMode}")
    public ResponseEntity<List<GameSession>> findGamesByMode(@PathVariable GameMode gameMode) {
        try {
            List<GameSession> sessions = gameSessionService.findGamesByMode(gameMode);
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @GetMapping("/FindByGS/{gamestateid}")
    public Object getGameSessionByGameStateId(@PathVariable String gamestateid) {
        try {
            GameSession session = gameSessionService.getGameSessionByGameStateId(gamestateid);
            return session;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }



}