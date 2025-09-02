package org.example.chessmystic.Service.interfaces.GameRelated;

import org.example.chessmystic.Models.Tracking.GameSession;
import org.springframework.transaction.annotation.Transactional;

// Real-time Service
public interface IRealtimeService {
    void broadcastGameState(String gameId);

    @Transactional
    GameSession createDelayedGameSession(String gameId, int delayPlies);

    void sendToPlayer(String playerId, Object message);
}

