package org.example.chessmystic.Models.KafkaEvents;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameEndEvent implements GameEvent {
    private String eventId;
    private String gameId;
    private String playerId;
    private long timestamp;
    private String eventType = "GAME_END";

    private String endReason;
    private String winnerId;
    private int totalMoves;
    private long gameDurationMs;
    private String finalPosition;
}