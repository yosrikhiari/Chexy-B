package org.example.chessmystic.Service.implementation.GameRelated;
import org.example.chessmystic.Models.GameStateandFlow.GameState;
import org.example.chessmystic.Models.GameStateandFlow.GameTimers;
import org.example.chessmystic.Models.GameStateandFlow.PlayerTimer;
import org.example.chessmystic.Models.Interactions.PlayerAction;
import org.example.chessmystic.Models.Tracking.GameHistory;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Models.chess.Piece;
import org.example.chessmystic.Models.chess.PieceColor;
import org.example.chessmystic.Models.chess.PieceType;
import org.example.chessmystic.Repository.GameSessionRepository;
import org.example.chessmystic.Repository.PlayerActionRepository;
import org.example.chessmystic.Service.interfaces.GameRelated.IRealtimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
public class RealtimeService implements IRealtimeService {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeService.class);
    private final SimpMessagingTemplate messagingTemplate;
    private static GameSessionService gameSessionService = null;
    static GameHistoryService gameHistoryService = null;
    private static PlayerActionRepository playerActionRepository = null;
    private final GameSessionRepository gameSessionRepository;

    @Autowired
    public RealtimeService(SimpMessagingTemplate messagingTemplate, GameSessionService gameSessionService, GameHistoryService gameHistoryService, PlayerActionRepository playerActionRepository, GameSessionRepository gameSessionRepository) {
        this.messagingTemplate = messagingTemplate;
        RealtimeService.gameSessionService = gameSessionService;
        RealtimeService.gameHistoryService = gameHistoryService;
        RealtimeService.playerActionRepository = playerActionRepository;
        this.gameSessionRepository = gameSessionRepository;
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
        long defaultMs = defaultSeconds * 1000L;
        long incrementMs = incrementSeconds * 1000L;

        // Start times
        Instant gameStart = session.getStartedAt() != null
                ? session.getStartedAt().atZone(ZoneId.systemDefault()).toInstant()
                : cutoff; // fallback if missing

        // Timer state (millisecond precision)
        long whiteMsLeft = defaultMs;
        long blackMsLeft = defaultMs;
        PieceColor active = PieceColor.white; // White starts

        // Walk through visible actions and subtract elapsed time from active player
        Instant lastTick = gameStart;
        for (PlayerAction a : visibleActions) {
            Instant t = a.getTimestamp().atZone(ZoneId.systemDefault()).toInstant();
            long elapsedMs = Math.max(0, Duration.between(lastTick, t).toMillis());

            if (active == PieceColor.white) {
                whiteMsLeft = Math.max(0, whiteMsLeft - elapsedMs);
                // apply increment to the player who just moved
                whiteMsLeft = Math.min(defaultMs, whiteMsLeft + incrementMs);
                active = PieceColor.black;
            } else {
                blackMsLeft = Math.max(0, blackMsLeft - elapsedMs);
                blackMsLeft = Math.min(defaultMs, blackMsLeft + incrementMs);
                active = PieceColor.white;
            }
            lastTick = t;
        }

        // Account for the in-progress interval from lastTick to cutoff on the current active player
        long tailMs = Math.max(0, Duration.between(lastTick, cutoff).toMillis());
        if (active == PieceColor.white) {
            whiteMsLeft = Math.max(0, whiteMsLeft - tailMs);
        } else {
            blackMsLeft = Math.max(0, blackMsLeft - tailMs);
        }

        // Compose timers
        // Round to nearest second to avoid systematic bias (prevents ~1-2s apparent lag)
        int whiteLeft = (int) Math.max(0, Math.round(whiteMsLeft / 1000.0));
        int blackLeft = (int) Math.max(0, Math.round(blackMsLeft / 1000.0));
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
                .serverTimeMs(Instant.now().toEpochMilli())
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


        GameSession delayed = new GameSession();
        // Keep only actions with timestamp <= cutoff
        List<PlayerAction> visibleActions = actions.stream()
                .filter(a -> a.getTimestamp() != null && a.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().isBefore(cutoff.plusMillis(1)))
                .toList();

        // 1) Reconstruct board by applying visibleActions starting from standard board
        Piece[][] delayedBoard = gameSessionService.initializeStandardChessBoard();
        for (PlayerAction a : visibleActions) {
            int fr = a.getFromX(), fc = a.getFromY();
            int tr = a.getToX(), tc = a.getToY();

            Piece moving = delayedBoard[fr][fc];
            if (moving == null) {
                // Defensive: skip malformed action
                continue;
            }

            // Move the piece by default
            delayedBoard[fr][fc] = null;
            delayedBoard[tr][tc] = moving;

            // Replicate special-move handling used in live path
            switch (a.getActionType()) {
                case CASTLE_KINGSIDE: {
                    // Rook moves from h-file to f-file on the same rank
                    int rookFromCol = 7;
                    int rookToCol = tc - 1;
                    Piece rook = delayedBoard[tr][rookFromCol];
                    if (rook != null) {
                        delayedBoard[tr][rookFromCol] = null;
                        delayedBoard[tr][rookToCol] = rook;
                    }
                    break;
                }
                case CASTLE_QUEENSIDE: {
                    // Rook moves from a-file to d-file on the same rank
                    int rookFromCol = 0;
                    int rookToCol = tc + 1;
                    Piece rook = delayedBoard[tr][rookFromCol];
                    if (rook != null) {
                        delayedBoard[tr][rookFromCol] = null;
                        delayedBoard[tr][rookToCol] = rook;
                    }
                    break;
                }
                case EN_PASSANT: {
                    // Remove the captured pawn behind the destination square
                    int direction = moving.getColor() == PieceColor.white ? 1 : -1;
                    int capturedRow = tr - direction;
                    if (capturedRow >= 0 && capturedRow < 8) {
                        delayedBoard[capturedRow][tc] = null;
                    }
                    break;
                }
                case PROMOTION: {
                    // Promote pawn to queen (default promotion in current implementation)
                    moving.setType(PieceType.QUEEN);
                    break;
                }
                case DOUBLE_PAWN_PUSH:
                case CAPTURE:
                case NORMAL:
                default:
                    // Already handled by default move
                    break;
            }
        }
        delayed.setBoard(delayedBoard);        // Apply visibleActions similarly to your existing switch(a.getActionType()) logic
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

    /**
     * Periodically recompute and broadcast delayed spectator timers and game state.
     * Ensures spectators receive a consistent delayed stream that matches the
     * move footprint and correct active clock.
     */
    @Scheduled(fixedRate = 1000)
    public void broadcastDelayedSpectatorTick() {
        try {
            // Iterate over active original sessions that allow spectators
            List<GameSession> activeSessions = gameSessionRepository.findByIsActiveTrue();
            for (GameSession original : activeSessions) {
                if (!original.isAllowSpectators()) continue;
                // Skip broadcasting if there are no spectators subscribed
                if (original.getSpectatorIds() == null || original.getSpectatorIds().isEmpty()) continue;

                String rawId = original.getGameId();
                // Build delayed snapshot at cutoff
                GameSession delayed = createTimeDelayedGameSession(rawId, Duration.ofMinutes(2));

                // Broadcast delayed timers to the spectator timer topic expected by the frontend
                String timerTopic = "/topic/game/SpecSession-" + rawId + "/timer";
                messagingTemplate.convertAndSend(timerTopic, delayed.getTimers());

                // Optionally broadcast delayed game state for UI turn indicators
                String stateTopic = "/topic/spectator-game-state/" + rawId;
                messagingTemplate.convertAndSend(stateTopic, delayed.getGameState());
            }
        } catch (Exception e) {
            logger.warn("Failed spectator delayed tick broadcast: {}", e.getMessage());
        }
    }
}