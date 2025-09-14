package org.example.chessmystic.Models.Analytics;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "game_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameAnalytics {
    @Id
    private String gameId;
    private String whitePlayerId;
    private String blackPlayerId;
    private String gameMode;
    private LocalDateTime gameStartTime;
    private LocalDateTime gameEndTime;
    private String winnerId;
    private int totalMoves;
    private int whiteMoves;
    private int blackMoves;
    private long gameDurationMs;
    private String endReason; // CHECKMATE, STALEMATE, RESIGNATION, TIMEOUT, etc.
    private List<String> openingMoves;
    private String detectedOpening;
    private int captureCount;
    private int checkCount;
    private boolean checkmate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
