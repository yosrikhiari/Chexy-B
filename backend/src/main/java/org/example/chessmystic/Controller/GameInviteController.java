package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.GameInvite;
import org.example.chessmystic.Models.GameInviteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class GameInviteController {
    private static final Logger log = LoggerFactory.getLogger(GameInviteController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game-invite/send")
    public void sendGameInvite(@Payload GameInvite invite) {
        log.info("Received game invite: {} -> {}", invite.getFromUsername(), invite.getToUserId());
        
        try {
            // Create response object
            GameInviteResponse response = new GameInviteResponse();
            response.setId(invite.getId());
            response.setFromUserId(invite.getFromUserId());
            response.setFromUsername(invite.getFromUsername());
            response.setToUserId(invite.getToUserId());
            response.setGameMode(invite.getGameMode());
            response.setIsRanked(invite.getIsRanked());
            response.setGameType(invite.getGameType());
            response.setTimestamp(invite.getTimestamp());
            response.setStatus("pending");

            // Send to the invited user
            messagingTemplate.convertAndSend("/queue/game-invites/" + invite.getToUserId(), response);
            
            log.info("Game invite sent successfully to user: {}", invite.getToUserId());
        } catch (Exception e) {
            log.error("Error sending game invite: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/game-invite/accept")
    public void acceptGameInvite(@Payload GameInviteAcceptRequest request) {
        log.info("Game invite accepted: {}", request.getInviteId());
        
        try {
            // Send acceptance notification to the original sender with complete game info
            GameInviteResponse response = new GameInviteResponse();
            response.setId(request.getInviteId());
            response.setStatus("accepted");
            response.setFromUserId(request.getUserId()); // The user who accepted
            response.setFromUsername(request.getFromUsername()); // Username of who accepted
            response.setToUserId(request.getFromUserId()); // The original sender
            response.setGameMode(request.getGameMode()); // Game mode
            response.setIsRanked(request.getIsRanked()); // Is ranked
            response.setGameType(request.getGameType()); // Game type
            response.setTimestamp(System.currentTimeMillis());
            
            // Send notification to the original sender that their invite was accepted
            messagingTemplate.convertAndSend("/queue/game-invite-response/" + request.getFromUserId(), response);
            
            // Send game ready notification to both players
            // This will be handled by the game session service when the inviter creates the session
            log.info("Game invite {} accepted by user: {} - notification sent to sender: {}", 
                    request.getInviteId(), request.getUserId(), request.getFromUserId());
        } catch (Exception e) {
            log.error("Error accepting game invite: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/game-invite/decline")
    public void declineGameInvite(@Payload GameInviteDeclineRequest request) {
        log.info("Game invite declined: {}", request.getInviteId());
        
        try {
            // Send decline notification to the original sender
            GameInviteResponse response = new GameInviteResponse();
            response.setId(request.getInviteId());
            response.setStatus("declined");
            
            // You would typically look up the original invite here to get the sender's ID
            // For now, we'll just log the decline
            log.info("Game invite {} declined by user: {}", request.getInviteId(), request.getUserId());
        } catch (Exception e) {
            log.error("Error declining game invite: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/game-ready/notify")
    public void notifyGameReady(@Payload GameReadyNotification request) {
        log.info("Game ready notification: gameId={}, inviterId={}, invitedId={}", 
                request.getGameId(), request.getInviterId(), request.getInvitedId());
        
        try {
            // Send game ready notification to the invited player
            messagingTemplate.convertAndSend("/queue/game-ready/" + request.getInvitedId(), request);
            
            log.info("Game ready notification sent to invited player: {}", request.getInvitedId());
        } catch (Exception e) {
            log.error("Error sending game ready notification: {}", e.getMessage(), e);
        }
    }

    // Simple request classes
    public static class GameInviteAcceptRequest {
        private String inviteId;
        private String userId;
        private String fromUserId; // ID of the user who sent the invite
        private String fromUsername; // Username of who accepted
        private org.example.chessmystic.Models.GameStateandFlow.GameMode gameMode;
        private boolean isRanked;
        private String gameType;

        public String getInviteId() { return inviteId; }
        public void setInviteId(String inviteId) { this.inviteId = inviteId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getFromUserId() { return fromUserId; }
        public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
        public String getFromUsername() { return fromUsername; }
        public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }
        public org.example.chessmystic.Models.GameStateandFlow.GameMode getGameMode() { return gameMode; }
        public void setGameMode(org.example.chessmystic.Models.GameStateandFlow.GameMode gameMode) { this.gameMode = gameMode; }
        public boolean getIsRanked() { return isRanked; }
        public void setIsRanked(boolean isRanked) { this.isRanked = isRanked; }
        public String getGameType() { return gameType; }
        public void setGameType(String gameType) { this.gameType = gameType; }
    }

    public static class GameInviteDeclineRequest {
        private String inviteId;
        private String userId;

        public String getInviteId() { return inviteId; }
        public void setInviteId(String inviteId) { this.inviteId = inviteId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public static class GameReadyNotification {
        private String gameId;
        private String inviterId;
        private String invitedId;
        private org.example.chessmystic.Models.GameStateandFlow.GameMode gameMode;
        private boolean isRanked;
        private String gameType;

        public String getGameId() { return gameId; }
        public void setGameId(String gameId) { this.gameId = gameId; }
        public String getInviterId() { return inviterId; }
        public void setInviterId(String inviterId) { this.inviterId = inviterId; }
        public String getInvitedId() { return invitedId; }
        public void setInvitedId(String invitedId) { this.invitedId = invitedId; }
        public org.example.chessmystic.Models.GameStateandFlow.GameMode getGameMode() { return gameMode; }
        public void setGameMode(org.example.chessmystic.Models.GameStateandFlow.GameMode gameMode) { this.gameMode = gameMode; }
        public boolean getIsRanked() { return isRanked; }
        public void setIsRanked(boolean isRanked) { this.isRanked = isRanked; }
        public String getGameType() { return gameType; }
        public void setGameType(String gameType) { this.gameType = gameType; }
    }
}
