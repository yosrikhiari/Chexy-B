package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.SpectatorChatMessage;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Service.implementation.GameRelated.ChessGameService;
import org.example.chessmystic.Service.implementation.GameRelated.GameSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Map;

@Controller
public class SpectatorController {
    private static final Logger log = LoggerFactory.getLogger(SpectatorController.class);


    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private GameSessionService gameSessionService;

    @MessageMapping("/spectator-chat/send")
    public void sendSpectatorMessage(@Payload SpectatorChatMessage message) {
        try {
            String destination = "/topic/spectator-chat/" + message.getGameId();
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.error("Error sending spectator message: {}", e.getMessage(), e);
        }
    }

    @PostMapping("/spectator/broadcast-delayed-state/{gameId}")
    public ResponseEntity<?> broadcastDelayedGameState(@PathVariable String gameId) {
        try {
            // Get delayed session for spectators
            GameSession delayedSession = gameSessionService.findById("SpecSession-" + gameId).orElse(null);
            if (delayedSession != null) {
                String destination = "/topic/spectator-game-state/" + gameId;
                messagingTemplate.convertAndSend(destination, delayedSession.getGameState());
                return ResponseEntity.ok(Map.of("message", "Delayed game state broadcasted"));
            }
            return ResponseEntity.ok(Map.of("message", "No delayed session available"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to broadcast delayed state"));
        }
    }

    @PostMapping("/spectator/broadcast-timer/{gameId}")
    public ResponseEntity<?> broadcastTimerUpdate(@PathVariable String gameId) {
        try {
            GameSession session = gameSessionService.findById(gameId).orElse(null);
            if (session != null) {
                String destination = "/topic/timer-updates/" + gameId;
                messagingTemplate.convertAndSend(destination, session.getTimers());
                return ResponseEntity.ok(Map.of("message", "Timer update broadcasted"));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Game session not found"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to broadcast timer update"));
        }
    }

    @PostMapping("/spectator/broadcast-count/{gameId}")
    public ResponseEntity<?> broadcastSpectatorCount(@PathVariable String gameId) {
        try {
            List<String> spectators = gameSessionService.getAllSpectators(gameId);
            String destination = "/topic/spectator-count/" + gameId;
            messagingTemplate.convertAndSend(destination, Map.of("count", spectators.size()));
            return ResponseEntity.ok(Map.of("message", "Spectator count broadcasted"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to broadcast spectator count"));
        }
    }
}


