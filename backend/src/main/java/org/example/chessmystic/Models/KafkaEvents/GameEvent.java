package org.example.chessmystic.Models.KafkaEvents;

public interface GameEvent {
    String getEventId();
    String getGameId();
    String getPlayerId();
    long getTimestamp();
    String getEventType();
}