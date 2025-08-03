package org.example.chessmystic.Models.Stats;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerGameStats {
    private int totalGamesPlayed;
    private int totalGamesWon;
    private int classicGamesPlayed;
    private int classicGamesWon;
    private int rpgGamesPlayed;
    private int rpgGamesWon;
    private int highestRPGRound;
    private int totalRPGScore;
    private LocalDateTime lastGamePlayed;
    private double winRate;
    private int currentStreak;
}