package org.example.chessmystic.Models;

import org.example.chessmystic.Models.GameStateandFlow.GameMode;

public class GameInviteResponse {
    private String id;
    private String fromUserId;
    private String fromUsername;
    private String toUserId;
    private GameMode gameMode;
    private boolean isRanked;
    private String gameType;
    private long timestamp;
    private String status; // "pending", "accepted", "declined"

    // Default constructor
    public GameInviteResponse() {}

    // Constructor with all fields
    public GameInviteResponse(String id, String fromUserId, String fromUsername, String toUserId, 
                             GameMode gameMode, boolean isRanked, String gameType, long timestamp, String status) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.fromUsername = fromUsername;
        this.toUserId = toUserId;
        this.gameMode = gameMode;
        this.isRanked = isRanked;
        this.gameType = gameType;
        this.timestamp = timestamp;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "GameInviteResponse{" +
                "id='" + id + '\'' +
                ", fromUserId='" + fromUserId + '\'' +
                ", fromUsername='" + fromUsername + '\'' +
                ", toUserId='" + toUserId + '\'' +
                ", gameMode=" + gameMode +
                ", isRanked=" + isRanked +
                ", gameType='" + gameType + '\'' +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                '}';
    }
}
