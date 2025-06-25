package org.example.chessmystic.Models.Stats;

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
@Document(collection = "player_profiles")
public class PlayerProfile {
    @Id
    private String id;

    // FIXED: Proper user reference naming and indexing
    @Indexed(unique = true)
    private String userId; // Changed from 'userid' to 'userId' for consistency

    private List<String> ownedPieceIds; // FIXED: More specific naming
    private List<String> ownedModifierIds; // FIXED: More specific naming
    private int totalCoins;
    private int highestScore;
    private int gamesPlayed;
    private int gamesWon;

    private PlayerGameStats gameStats;

    // FIXED: Add missing fields for better profile management
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private String currentGameSessionId; // Link to active game
    private List<String> gameHistoryIds; // Links to GameHistory records
}