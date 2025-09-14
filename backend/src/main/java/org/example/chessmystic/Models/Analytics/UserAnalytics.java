package org.example.chessmystic.Models.Analytics;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "user_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAnalytics {
    @Id
    private String userId;
    private int totalGamesPlayed;
    private int gamesWon;
    private int gamesLost;
    private int gamesDrawn;
    private double winRate;
    private int totalMovesPlayed;
    private long totalGameTimeMs;
    private String mostPlayedGameMode;
    private String favoriteOpening;
    private int averageGameDurationMinutes;
    private int longestWinStreak;
    private int currentWinStreak;
    private Map<String, Integer> actionCounts; // LOGIN, LOGOUT, GAME_JOIN, etc.
    private LocalDateTime lastActivityTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
