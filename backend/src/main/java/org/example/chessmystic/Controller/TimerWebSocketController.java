package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Models.GameStateandFlow.GameStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class TimerWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/timer/{gameId}")
    public void handleTimerUpdate(@DestinationVariable String gameId, String message) {
        // Optional: Handle client-initiated timer messages if needed
    }

    public void broadcastTimerUpdate(String gameId, GameSession session) {
        // Only send timer updates for active games
        if (session.getStatus() == GameStatus.ACTIVE && session.isActive()) {
            // Use explicit amq.topic and dot-separated routing key to be RabbitMQ STOMP compatible
            messagingTemplate.convertAndSend("/exchange/amq.topic/game." + gameId + ".timer", session.getTimers());
        } else {
            // Log when trying to send timer updates for non-active games
            System.out.println("Skipping timer update for game " + gameId + " - status: " + session.getStatus() + ", active: " + session.isActive());
        }
    }
}