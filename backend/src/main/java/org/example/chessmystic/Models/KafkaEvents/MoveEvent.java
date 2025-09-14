package org.example.chessmystic.Models.KafkaEvents;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveEvent implements GameEvent {
    private String eventId;
    private String gameId;
    private String playerId;
    private long timestamp;
    private String eventType = "MOVE";

    // Move specific data
    private int fromRow, fromCol, toRow, toCol;
    private String pieceType;
    private String pieceColor;
    private boolean isCapture;
    private boolean isCheck;
    private boolean isCheckmate;
    private long moveTimeMs;
}