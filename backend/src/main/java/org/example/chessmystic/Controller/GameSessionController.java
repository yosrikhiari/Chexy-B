package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Models.GameStateandFlow.GameState;
import org.example.chessmystic.Models.GameStateandFlow.GameStatus;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Models.UIUX.TieResolutionOption;
import org.example.chessmystic.Service.implementation.GameRelated.MatchmakingService;
import org.example.chessmystic.Service.interfaces.GameRelated.IGameSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static org.example.chessmystic.Service.implementation.GameRelated.GameSessionService.logger;

@RestController
@RequestMapping("/game-session")
public class GameSessionController {

    private final IGameSessionService gameSessionService;
    private final MatchmakingService matchmakingService;


    public GameSessionController(IGameSessionService gameSessionService, MatchmakingService matchmakingService) {
        this.gameSessionService = gameSessionService;
        this.matchmakingService = matchmakingService;
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
            if (session.isPresent()) {
                logger.info("Returning game session for gameId {}: {}", gameId, session.get());
                return ResponseEntity.ok(session.get());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Game session not found"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to find game session", "message", e.getMessage()));
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

    @MessageMapping("/matchmaking/accept")
    public void acceptMatch(@Payload Map<String, String> payload) {
        String matchId = payload.get("matchId");
        String userId = payload.get("userId");
        matchmakingService.acceptMatch(matchId, userId);
    }

    @MessageMapping("/matchmaking/decline")
    public void declineMatch(@Payload Map<String, String> payload) {
        String matchId = payload.get("matchId");
        String userId = payload.get("userId");
        matchmakingService.declineMatch(matchId, userId);
    }

    @MessageMapping("/matchmaking/join")
    public void joinMatchmaking(Map<String, Object> payload) {
        String userId = (String) payload.get("userId");
        int points = ((Number) payload.get("points")).intValue();
        matchmakingService.joinQueue(userId, points);
    }

    @MessageMapping("/matchmaking/leave")
    public void leaveMatchmaking(Map<String, String> payload) {
        String userId = payload.get("userId");
        matchmakingService.leaveQueue(userId);
    }


    @GetMapping("/api/matchmaking/status")
    public ResponseEntity<?> getMatchmakingStatus() {
        try {
            int playersInQueue = matchmakingService.getQueueSize();
            return ResponseEntity.ok(Map.of("playersInQueue", playersInQueue));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get matchmaking status"));
        }
    }


    @PostMapping("/{gameId}/Spectate/join/{playerId}")
    public void joinSpectate(@PathVariable String gameId, @PathVariable String playerId) {
        try{
            gameSessionService.isJoinedSpectating(gameId,playerId);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @PostMapping("/{gameId}/Spectate/leave/{playerId}")
    public void leaveSpectate(@PathVariable String gameId, @PathVariable String playerId) {
        try{
            gameSessionService.isLeftSpectating(gameId,playerId);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @GetMapping("/{gameId}/Spectators")
    public ResponseEntity<List<String>> getSpectators(@PathVariable String gameId) {
        try{
            List<String> spectators = gameSessionService.getAllSpectators(gameId);
            return ResponseEntity.ok(spectators);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @PostMapping("/{gameId}/mode/off")
    public void offSpectators(@PathVariable String gameId) {
        try{
            gameSessionService.offSpectatorMode(gameId);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    @PostMapping("/{gameId}/mode/on")
    public void onSpectators(@PathVariable String gameId) {
        try{
            gameSessionService.onSpectatorMode(gameId);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

}