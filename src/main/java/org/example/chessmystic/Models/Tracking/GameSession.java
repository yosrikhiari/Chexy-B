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
import java.util.ArrayList;
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


    // PLAYER MANAGEMENT - Improved structure - that contains the id of the players
    private PlayerSessionInfo whitePlayer;
    private List<PlayerSessionInfo> blackPlayer;

    // GAME CONFIGURATION
    private GameMode gameMode;
    private boolean isRankedMatch;
    private boolean isPrivate;
    private String inviteCode;

    // CURRENT BOARD STATE - i'm skeptical about this
    private Piece[][] board;

    // One of These Three Has a Value or in case it's the enhancedgamestate it's two of the 3 has a value
    private GameState gameState;
    private String rpgGameStateId;
    private String enhancedGameStateId;

    private GameTimers timers;

    // SESSION MANAGEMENT - Enhanced
    @Indexed private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime lastActivity;
    private boolean isActive;
    @Indexed private GameStatus status;
    // it's a map of the id of players and the date of the last time they connected
    private Map<String, LocalDateTime> playerLastSeen;

    // GAME SETTINGS
    private int timeControlMinutes;
    private int incrementSeconds;
    private boolean allowSpectators;
    private List<String> spectatorIds;



    private String gameHistoryId; // Link to GameHistory record
    private List<String> moveHistoryIds; // Links to PlayerAction records

    public List<String> getPlayerIds() {
        List<String> playerIds = new ArrayList<>();
        if (whitePlayer != null) {
            playerIds.add(whitePlayer.getUserId());
        }
        if (blackPlayer != null && !blackPlayer.isEmpty()) {
            for (PlayerSessionInfo player : blackPlayer) {
                playerIds.add(player.getUserId());
            }
        }
        return playerIds;
    }
    public PlayerSessionInfo getCurrentPlayer() {
        // Check white player first
        if (whitePlayer != null && whitePlayer.isCurrentTurn()) {
            return whitePlayer;
        }

        // Check black players
        if (blackPlayer != null) {
            return blackPlayer.stream()
                    .filter(PlayerSessionInfo::isCurrentTurn)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }
    public String getCurrentPlayerId() {
        PlayerSessionInfo currentPlayer = getCurrentPlayer();
        return currentPlayer != null ? currentPlayer.getUserId() : null;
    }
    public boolean isCurrentPlayer(String playerId) {
        PlayerSessionInfo currentPlayer = getCurrentPlayer();
        return currentPlayer != null && currentPlayer.getUserId().equals(playerId);
    }

    public void setCurrentTurn(String playerId) {
        // Reset all players' turn status
        if (whitePlayer != null) {
            whitePlayer.setCurrentTurn(false);
        }
        if (blackPlayer != null) {
            blackPlayer.forEach(player -> player.setCurrentTurn(false));
        }

        // Set the specified player's turn to true
        if (whitePlayer != null && whitePlayer.getUserId().equals(playerId)) {
            whitePlayer.setCurrentTurn(true);
        } else if (blackPlayer != null) {
            blackPlayer.stream()
                    .filter(player -> player.getUserId().equals(playerId))
                    .findFirst()
                    .ifPresent(player -> player.setCurrentTurn(true));
        }
    }

}

