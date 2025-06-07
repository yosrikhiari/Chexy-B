package org.example.chessmystic.Models.Mechanics;

import lombok.*;
import org.example.chessmystic.Models.Transactions.CapacityModifier;
import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Models.GameStateandFlow.GameStatus;
import org.example.chessmystic.Models.Transactions.RPGBoardModifier;
import org.example.chessmystic.Models.Transactions.RPGModifier;
import org.example.chessmystic.Models.rpg.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "rpg_game_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RPGGameState {
    @Id
    private String gameId;

    // GAME PROGRESSION
    private int currentRound;
    private List<RPGPiece> playerArmy;
    private List<RPGModifier> activeModifiers;
    private List<RPGBoardModifier> activeBoardModifiers;
    private List<CapacityModifier> activeCapacityModifiers;
    private List<BoardEffect> boardEffects;
    private int boardSize;
    private ArmyCapacity armyCapacity;
    private List<Integer> completedRounds;
    private int lives;
    private int score;
    private int coins;
    private boolean isGameOver;
    private String currentObjective;
    private Integer turnsRemaining;

    // SESSION MANAGEMENT
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private LocalDateTime lastPlayerActivity;

    // FIXED: Proper session linking
    @Indexed
    private String gameSessionId; // For reconnection - should match GameSession.gameId
    private GameMode gameMode;
    private GameStatus status;

    // MULTIPLAYER SUPPORT (when applicable)
    private boolean isPlayerTurn;

    // FIXED: Add missing links for proper game management
    private String currentRoundConfigId; // Link to BoardConfiguration
    private List<String> actionHistoryIds; // Links to PlayerAction records
    private String shopStateId; // Link to current shop state if applicable
}