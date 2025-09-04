package org.example.chessmystic.Service.interfaces.GameRelated;

import org.example.chessmystic.Models.Tracking.GameSession;
import org.springframework.transaction.annotation.Transactional;

// Real-time Service
public interface IRealtimeService {
    void broadcastGameState(String gameId);


    void sendToPlayer(String playerId, Object message);
}

