package org.example.chessmystic.Service.interfaces.GameRelated;

// Real-time Service
public interface IRealtimeService {
    void broadcastGameState(String gameId);
    void sendToPlayer(String playerId, Object message);
}

