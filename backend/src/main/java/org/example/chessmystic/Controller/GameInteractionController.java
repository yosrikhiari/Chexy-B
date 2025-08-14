package org.example.chessmystic.Controller;

import org.example.chessmystic.Service.implementation.GameRelated.GameSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
public class GameInteractionController {

    private static final Logger logger = LoggerFactory.getLogger(GameInteractionController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final GameSessionService gameSessionService;

    @Autowired
    public GameInteractionController(SimpMessagingTemplate messagingTemplate,
                                     GameSessionService gameSessionService) {
        this.messagingTemplate = messagingTemplate;
        this.gameSessionService = gameSessionService;
    }

    @MessageMapping("/game/draw/offer")
    public void offerDraw(@Payload Map<String, Object> payload) {
        try {
            String gameId = (String) payload.get("gameId");
            String fromUserId = (String) payload.get("fromUserId");
            String fromUsername = (String) payload.get("fromUsername");
            String toUserId = (String) payload.get("toUserId");

            logger.info("Draw offer received: gameId={}, fromUserId={} -> toUserId={}", gameId, fromUserId, toUserId);

            Map<String, Object> message = new HashMap<>();
            message.put("gameId", gameId);
            message.put("fromUserId", fromUserId);
            message.put("fromUsername", fromUsername);

            // Notify the opponent about the draw offer on a per-user destination (path includes userId)
            messagingTemplate.convertAndSend("/queue/game/draw/offer/" + toUserId, message);
        } catch (Exception e) {
            logger.error("Failed to process draw offer message: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/game/draw/response")
    public void respondToDraw(@Payload Map<String, Object> payload) {
        try {
            String gameId = (String) payload.get("gameId");
            String fromUserId = (String) payload.get("fromUserId"); // responder
            String toUserId = (String) payload.get("toUserId"); // original offerer
            boolean accepted = Boolean.parseBoolean(String.valueOf(payload.get("accepted")));

            logger.info("Draw response received: gameId={}, fromUserId={}, toUserId={}, accepted={}",
                    gameId, fromUserId, toUserId, accepted);

            Map<String, Object> ack = new HashMap<>();
            ack.put("gameId", gameId);
            ack.put("fromUserId", fromUserId);
            ack.put("accepted", accepted);

            if (accepted) {
                // End the game as a draw (no points) on the backend
                try {
                    gameSessionService.endGame(gameId, null, true, null);
                } catch (Exception e) {
                    logger.warn("Failed to end game on draw acceptance; it may already be completed: {}", e.getMessage());
                }

                // Notify both players of acceptance on per-user destinations
                messagingTemplate.convertAndSend("/queue/game/draw/accepted/" + toUserId, ack);
                messagingTemplate.convertAndSend("/queue/game/draw/accepted/" + fromUserId, ack);
            } else {
                // Notify the original offerer that the draw was declined
                messagingTemplate.convertAndSend("/queue/game/draw/declined/" + toUserId, ack);
            }
        } catch (Exception e) {
            logger.error("Failed to process draw response message: {}", e.getMessage(), e);
        }
    }
}


