package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Service.interfaces.GameRelated.IRealtimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealtimeService implements IRealtimeService {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeService.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final GameSessionService gameSessionService;

    @Autowired
    public RealtimeService(SimpMessagingTemplate messagingTemplate, GameSessionService gameSessionService) {
        this.messagingTemplate = messagingTemplate;
        this.gameSessionService = gameSessionService;
    }

    @Override
    public void broadcastGameState(String gameId) {
        GameSession session = gameSessionService.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));

        logger.info("Broadcasting game state for game: {}", gameId);
        messagingTemplate.convertAndSend("/topic/game/" + gameId, session.getGameState());
        if (session.getSpectatorIds() != null) {
            session.getSpectatorIds().forEach(spectatorId ->
                    messagingTemplate.convertAndSendToUser(spectatorId, "/queue/game/" + gameId, session.getGameState()));
        }
    }

    @Override
    public void sendToPlayer(String playerId, Object message) {
        logger.info("Sending message to player: {}", playerId);
        messagingTemplate.convertAndSendToUser(playerId, "/queue/messages", message);
    }
}