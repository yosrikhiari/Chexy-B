package org.example.chessmystic.Controller;

import org.example.chessmystic.Service.interfaces.GameRelated.IRealtimeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/realtime")
public class RealtimeController {

    private final IRealtimeService realtimeService;

    public RealtimeController(IRealtimeService realtimeService) {
        this.realtimeService = realtimeService;
    }


    @PostMapping("/broadcast/{gameId}")
    public ResponseEntity<?> broadcastGameState(@PathVariable String gameId) {
        try {
            realtimeService.broadcastGameState(gameId);
            return ResponseEntity.ok(Map.of("message", "Game state broadcasted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to broadcast game state", "message", "An unexpected error occurred"));
        }
    }

    @PostMapping("/send/{playerId}")
    public ResponseEntity<?> sendToPlayer(@PathVariable String playerId, @RequestBody Object message) {
        try {
            realtimeService.sendToPlayer(playerId, message);
            return ResponseEntity.ok(Map.of("message", "Message sent to player successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send message", "message", "An unexpected error occurred"));
        }
    }
}