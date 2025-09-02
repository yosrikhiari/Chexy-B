package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Service.interfaces.GameRelated.IRealtimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RealtimeService implements IRealtimeService {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeService.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final GameSessionService gameSessionService;

    @Autowired
    public RealtimeService(SimpMessagingTemplate messagingTemplate, GameSessionService gameSessionService) {
        this.messagingTemplate = messagingTemplate;
        this.gameSessionService = gameSessionService;
    }

    @Override
    public void broadcastGameState(String gameId) {
        GameSession session = gameSessionService.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));

        logger.info("Broadcasting game state for game: {}", gameId);
        messagingTemplate.convertAndSend("/topic/game/" + gameId, session.getGameState());
        if (session.getSpectatorIds() != null) {
            session.getSpectatorIds().forEach(spectatorId ->
                    messagingTemplate.convertAndSendToUser(spectatorId, "/queue/game/" + gameId, session.getGameState()));
        }
    }

    /**
     * Creates a delayed copy of the game session for spectators
     * @param gameId The game ID
     * @param delayPlies Number of plies (half-moves) to delay (default: 2)
     * @return A copy of the game session with delayed move history, or null if not enough moves
     */
    @Transactional
    @Override
    public GameSession createDelayedGameSession(String gameId, int delayPlies) {
        // Find the original game session
        GameSession originalSession = gameSessionService.findById(gameId)
                .orElseThrow(() -> {
                    System.err.println("Game session not found: " + gameId);
                    return new IllegalArgumentException("Game session not found");
                });

        // Get the move history
        List<String> moveHistoryIds = originalSession.getMoveHistoryIds();

        // Check if there are enough moves to create a delay
        if (moveHistoryIds == null || moveHistoryIds.size() <= delayPlies) {
            // Not enough moves to create delay, return null
            return null;
        }

        try {
            // Create a NEW GameSession object (don't modify the original!)
            GameSession delayedSession = new GameSession();

            // Copy all the basic fields from the original session
            delayedSession.setGameId(originalSession.getGameId());
            delayedSession.setWhitePlayer(originalSession.getWhitePlayer());
            delayedSession.setBlackPlayer(originalSession.getBlackPlayer());
            delayedSession.setGameMode(originalSession.getGameMode());
            delayedSession.setRankedMatch(originalSession.isRankedMatch());
            delayedSession.setPrivate(originalSession.isPrivate());
            delayedSession.setInviteCode(originalSession.getInviteCode());
            delayedSession.setGameState(originalSession.getGameState());
            delayedSession.setRpgGameStateId(originalSession.getRpgGameStateId());
            delayedSession.setEnhancedGameStateId(originalSession.getEnhancedGameStateId());
            delayedSession.setTimers(originalSession.getTimers());
            delayedSession.setCreatedAt(originalSession.getCreatedAt());
            delayedSession.setStartedAt(originalSession.getStartedAt());
            delayedSession.setLastActivity(originalSession.getLastActivity());
            delayedSession.setActive(originalSession.isActive());
            delayedSession.setStatus(originalSession.getStatus());
            delayedSession.setPlayerLastSeen(originalSession.getPlayerLastSeen());
            delayedSession.setTimeControlMinutes(originalSession.getTimeControlMinutes());
            delayedSession.setIncrementSeconds(originalSession.getIncrementSeconds());
            delayedSession.setAllowSpectators(originalSession.isAllowSpectators());
            delayedSession.setSpectatorIds(originalSession.getSpectatorIds());
            delayedSession.setBotId(originalSession.getBotId());
            delayedSession.setGameHistoryId(originalSession.getGameHistoryId());

            // Create a NEW list for the delayed move history (don't modify the original!)
            List<String> delayedMoveHistory = new ArrayList<>();

            // Add all moves EXCEPT the last 'delayPlies' moves
            for (int i = 0; i < moveHistoryIds.size() - delayPlies; i++) {
                delayedMoveHistory.add(moveHistoryIds.get(i));
            }

            // Set the delayed move history on the new session
            delayedSession.setMoveHistoryIds(delayedMoveHistory);

            // IMPORTANT: You'll also need to reconstruct the board state
            // based on the delayed move history, not the current board
            // This depends on how you want to handle the board reconstruction

            return delayedSession;

        } catch (Exception e) {
            System.err.println("Error creating delayed game session: " + e.getMessage());
            return null;
        }
    }




    @Override
    public void sendToPlayer(String playerId, Object message) {
        logger.info("Sending message to player: {}", playerId);
        messagingTemplate.convertAndSendToUser(playerId, "/queue/messages", message);
    }
}