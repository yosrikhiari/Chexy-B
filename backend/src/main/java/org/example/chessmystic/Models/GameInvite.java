package org.example.chessmystic.Models;

import org.example.chessmystic.Models.GameStateandFlow.GameMode;

public class GameInvite {
    private String id;
    private String fromUserId;
    private String fromUsername;
    private String toUserId;
    private GameMode gameMode;
    private boolean isRanked;
    private String gameType;
    private long timestamp;

    // Default constructor
    public GameInvite() {}

    // Constructor with all fields
    public GameInvite(String id, String fromUserId, String fromUsername, String toUserId, 
                     GameMode gameMode, boolean isRanked, String gameType, long timestamp) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.fromUsername = fromUsername;
        this.toUserId = toUserId;
        this.gameMode = gameMode;
        this.isRanked = isRanked;
        this.gameType = gameType;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }

    public String getToUserId() {
        return toUserId;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public boolean getIsRanked() {
        return isRanked;
    }

    public void setIsRanked(boolean isRanked) {
        this.isRanked = isRanked;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "GameInvite{" +
                "id='" + id + '\'' +
                ", fromUserId='" + fromUserId + '\'' +
                ", fromUsername='" + fromUsername + '\'' +
                ", toUserId='" + toUserId + '\'' +
                ", gameMode=" + gameMode +
                ", isRanked=" + isRanked +
                ", gameType='" + gameType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
