package org.example.chessmystic.Models.KafkaEvents;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameStartEvent implements GameEvent {
    private String eventId;
    private String gameId;
    private String playerId;
    private long timestamp;
    private String eventType = "GAME_START";

    private String opponentId;
    private String gameMode;
    private int timeControlMinutes;
    private String openingDetected;

}