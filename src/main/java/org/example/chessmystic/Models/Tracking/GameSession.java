package org.example.chessmystic.Models.Tracking;

import lombok.*;
import org.example.chessmystic.Models.GameStateandFlow.GameState;
import org.example.chessmystic.Models.GameStateandFlow.GameTimers;
import org.example.chessmystic.Models.chess.Piece;
import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Models.GameStateandFlow.GameStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "game_sessions")
@CompoundIndex(def = "{'playerIds': 1, 'status': 1}")
@CompoundIndex(def = "{'createdAt': -1, 'gameMode': 1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameSession {
    @Id
    private String gameId;

    // PLAYER MANAGEMENT - Improved structure
    private PlayerSessionInfo whitePlayer;
    private PlayerSessionInfo blackPlayer;

    // GAME CONFIGURATION
    private GameMode gameMode;
    private boolean isRankedMatch;
    private boolean isPrivate;
    private String inviteCode;

    // CURRENT BOARD STATE
    private Piece[][] board;
    private GameState gameState;
    private GameTimers timers;

    // SESSION MANAGEMENT - Enhanced
    @Indexed private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime lastActivity;
    private boolean isActive;
    @Indexed private GameStatus status;
    private Map<String, LocalDateTime> playerLastSeen;

    // GAME SETTINGS
    private int timeControlMinutes;
    private int incrementSeconds;
    private boolean allowSpectators;
    private List<String> spectatorIds;

    // FIXED: Proper RPG mode integration with specific references
    private String rpgGameStateId; // Link to RPGGameState when in RPG mode
    private String enhancedGameStateId; // Link to EnhancedGameState when in Enhanced RPG mode

    // FIXED: Add game history tracking
    private String gameHistoryId; // Link to GameHistory record
    private List<String> moveHistoryIds; // Links to PlayerAction records

    // FIXED: Add indexing for common queries
    @Indexed
    private List<String> playerIds; // All participating player IDs for easy querying



}


