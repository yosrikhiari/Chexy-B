package org.example.chessmystic.Service.implementation.GameRelated;

import org.apache.commons.lang3.function.TriConsumer;
import org.example.chessmystic.Models.GameStateandFlow.GameState;
import org.example.chessmystic.Models.GameStateandFlow.GameTimers;
import org.example.chessmystic.Models.GameStateandFlow.PlayerTimer;
import org.example.chessmystic.Models.Interactions.PlayerAction;
import org.example.chessmystic.Models.Tracking.GameHistory;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Models.chess.BoardPosition;
import org.example.chessmystic.Models.chess.Piece;
import org.example.chessmystic.Models.chess.PieceColor;
import org.example.chessmystic.Repository.PlayerActionRepository;
import org.example.chessmystic.Service.interfaces.GameRelated.IRealtimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Service
public class RealtimeService implements IRealtimeService {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeService.class);
    private final SimpMessagingTemplate messagingTemplate;
    private static GameSessionService gameSessionService = null;
    static GameHistoryService gameHistoryService = null;
    private static PlayerActionRepository playerActionRepository = null;

    @Autowired
    public RealtimeService(SimpMessagingTemplate messagingTemplate, GameSessionService gameSessionService, GameHistoryService gameHistoryService, PlayerActionRepository playerActionRepository) {
        this.messagingTemplate = messagingTemplate;
        RealtimeService.gameSessionService = gameSessionService;
        RealtimeService.gameHistoryService = gameHistoryService;
        RealtimeService.playerActionRepository = playerActionRepository;
    }

    @Override
    public void broadcastGameState(String gameId) {
        GameSession session = gameSessionService.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));

        logger.info("Broadcasting game state for game: {}", gameId);
        messagingTemplate.convertAndSend("/topic/broadcast/" + gameId, session.getGameState());
        if (session.getSpectatorIds() != null) {
            session.getSpectatorIds().forEach(spectatorId ->
                    messagingTemplate.convertAndSendToUser(spectatorId, "/queue/game/" + gameId, session.getGameState()));
        }
    }

    private static GameTimers computeTimersAtCutoff(GameSession session,
                                                    List<PlayerAction> allActions,
                                                    List<PlayerAction> visibleActions,
                                                    Instant cutoff) {
        // Defaults
        int defaultSeconds = session.getTimeControlMinutes() * 60;
        int incrementSeconds = session.getIncrementSeconds() != 0 ? session.getIncrementSeconds() : 0;

        // Start times
        Instant gameStart = session.getStartedAt() != null
                ? session.getStartedAt().atZone(ZoneId.systemDefault()).toInstant()
                : cutoff; // fallback if missing

        // Timer state
        int whiteLeft = defaultSeconds;
        int blackLeft = defaultSeconds;
        PieceColor active = PieceColor.white; // White starts

        // Walk through visible actions and subtract elapsed time from active player
        Instant lastTick = gameStart;
        for (PlayerAction a : visibleActions) {
            Instant t = a.getTimestamp().atZone(ZoneId.systemDefault()).toInstant();
            long elapsed = Math.max(0, Duration.between(lastTick, t).getSeconds());

            if (active == PieceColor.white) {
                whiteLeft = Math.max(0, whiteLeft - (int) elapsed);
                // apply increment to the player who just moved
                whiteLeft = Math.min(defaultSeconds, whiteLeft + incrementSeconds);
                active = PieceColor.black;
            } else {
                blackLeft = Math.max(0, blackLeft - (int) elapsed);
                blackLeft = Math.min(defaultSeconds, blackLeft + incrementSeconds);
                active = PieceColor.white;
            }
            lastTick = t;
        }

        // Account for the in-progress interval from lastTick to cutoff on the current active player
        long tail = Math.max(0, Duration.between(lastTick, cutoff).getSeconds());
        if (active == PieceColor.white) {
            whiteLeft = Math.max(0, whiteLeft - (int) tail);
        } else {
            blackLeft = Math.max(0, blackLeft - (int) tail);
        }

        // Compose timers
        var white = PlayerTimer.builder()
                .timeLeft(whiteLeft)
                .active(active == PieceColor.white)
                .build();
        var black = PlayerTimer.builder()
                .timeLeft(blackLeft)
                .active(active == PieceColor.black)
                .build();

        return GameTimers.builder()
                .defaultTime(defaultSeconds)
                .white(white)
                .black(black)
                .build();
    }

    /**
     * Creates a delayed copy of the game session for spectators
     * @param gameId The game ID
     * @param delay Time of delay (default: 2)
     * @return A copy of the game session with delayed move history, or null if not enough moves
     */

    public static GameSession createTimeDelayedGameSession(String gameId, Duration delay) {
        GameSession original = gameSessionService.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found"));

        Instant cutoff = Instant.now().minus(delay);
        GameHistory history = gameHistoryService.findByGameSessionId(gameId)
                .orElseThrow(() -> new RuntimeException("Game history not found"));

        // Load and sort actions by sequenceNumber or timestamp (sequenceNumber should already be ordered)
        List<PlayerAction> actions = history.getPlayerActionIds().stream()
                .map(id -> playerActionRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Player action not found: " + id)))
                .sorted(Comparator.comparingInt(PlayerAction::getSequenceNumber)) // safe if sequence is consistent
                .toList();

        // Keep only actions with timestamp <= cutoff
        List<PlayerAction> visibleActions = actions.stream()
                .filter(a -> a.getTimestamp() != null && a.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().isBefore(cutoff.plusMillis(1)))
                .toList();

        // 1) Reconstruct board by applying visibleActions starting from standard board
        Piece[][] delayedBoard = gameSessionService.initializeStandardChessBoard();
        // Apply visibleActions similarly to your existing switch(a.getActionType()) logic
        // (reuse your reconstruction code from createDelayedGameSession)

        // 2) Compute turn at cutoff
        PieceColor turn = (visibleActions.size() % 2 == 0) ? PieceColor.white : PieceColor.black;

        // 3) Recompute timers at cutoff
        GameTimers delayedTimers = (GameTimers) computeTimersAtCutoff(original, actions, visibleActions, cutoff);

        // 4) Build delayed GameState (clone minimal fields)
        GameState delayedState = GameState.builder()
                .gamestateId(original.getGameState().getGamestateId())
                .gameSessionId("SpecSession-" + original.getGameId())
                .currentTurn(turn)
                .moveCount(visibleActions.size() / 2)
                .isGameOver(false)
                .isCheck(false)
                .isCheckmate(false)
                .canWhiteCastleKingSide(false)
                .canWhiteCastleQueenSide(false)
                .canBlackCastleKingSide(false)
                .canBlackCastleQueenSide(false)
                .enPassantTarget(null)
                .build();

        // 5) Build delayed session
        GameSession delayed = new GameSession();
        delayed.setGameId("SpecSession-" + original.getGameId());
        delayed.setWhitePlayer(original.getWhitePlayer());
        delayed.setBlackPlayer(original.getBlackPlayer());
        delayed.setGameMode(original.getGameMode());
        delayed.setRankedMatch(original.isRankedMatch());
        delayed.setPrivate(original.isPrivate());
        delayed.setInviteCode(original.getInviteCode());
        delayed.setGameState(delayedState);
        delayed.setRpgGameStateId(original.getRpgGameStateId());
        delayed.setEnhancedGameStateId(original.getEnhancedGameStateId());
        delayed.setTimers((org.example.chessmystic.Models.GameStateandFlow.GameTimers) delayedTimers);
        delayed.setCreatedAt(original.getCreatedAt());
        delayed.setStartedAt(original.getStartedAt());
        delayed.setLastActivity(original.getLastActivity());
        delayed.setActive(original.isActive());
        delayed.setStatus(original.getStatus());
        delayed.setPlayerLastSeen(original.getPlayerLastSeen());
        delayed.setTimeControlMinutes(original.getTimeControlMinutes());
        delayed.setIncrementSeconds(original.getIncrementSeconds());
        delayed.setAllowSpectators(original.isAllowSpectators());
        delayed.setSpectatorIds(original.getSpectatorIds());
        delayed.setBotId(original.getBotId());
        delayed.setGameHistoryId(original.getGameHistoryId());
        delayed.setBoard(delayedBoard);
        delayed.setMoveHistoryIds(
                visibleActions.stream().map(PlayerAction::getId).toList()
        );

        return delayed;
    }



    @Override
    public void sendToPlayer(String playerId, Object message) {
        logger.info("Sending message to player: {}", playerId);
        messagingTemplate.convertAndSendToUser(playerId, "/queue/messages", message);
    }
}