package org.example.chessmystic.Models.Interactions;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "player_actions")
public class PlayerAction {
    @Id
    private String id;

    // FIXED: Consistent session references
    @Indexed
    private String gameSessionId; // Should match GameSession.gameId
    private String rpgGameStateId; // Optional - only for RPG games
    private String enhancedGameStateId; // Optional - only for Enhanced RPG games

    @Indexed
    private String playerId; // Link to User.id

    private ActionType actionType; //
    private int fromX;
    private int fromY;
    private int toX;
    private int toY;

    @Indexed
    private LocalDateTime timestamp;

    private String abilityUsed;
    private int damageDealt;
    private boolean isCriticalHit;

    // FIXED: Add sequence number for proper move ordering
    private int sequenceNumber;
    private int roundNumber; // For RPG games
}