package org.example.chessmystic.Models.Tracking;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "game_histories")
public class GameHistory {
    @Id
    private String id;

    @Indexed
    private String gameSessionId; // FIXED: Link to GameSession.gameId

    @Indexed
    private List<String> userIds; // All participating users
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private GameResult result;

    // FIXED: Better move tracking
    private List<String> playerActionIds; // Links to PlayerAction records
    private int totalMoves;

    private boolean isRanked;
    private boolean isRPGMode;
    private int finalRound;
    private int finalScore;

}