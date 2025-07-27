package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.Tracking.GameSession;
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
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/timer", session.getTimers());
    }
}