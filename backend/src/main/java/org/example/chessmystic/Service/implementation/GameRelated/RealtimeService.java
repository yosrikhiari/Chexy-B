package org.example.chessmystic.Service.implementation.GameRelated;

import org.apache.commons.lang3.function.TriConsumer;
import org.example.chessmystic.Models.GameStateandFlow.GameState;
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

    /**
     * Creates a delayed copy of the game session for spectators
     * @param gameId The game ID
     * @param delayPlies Number of plies (half-moves) to delay (default: 2)
     * @return A copy of the game session with delayed move history, or null if not enough moves
     */

    public static GameSession createDelayedGameSession(String gameId, int delayPlies) {
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
            delayedSession.setGameId("SpecSession-"+originalSession.getGameId());
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
            Piece[][] delayedboard = gameSessionService.initializeStandardChessBoard() ;

            GameHistory gameHistory = gameHistoryService.findByGameSessionId(originalSession.getGameId())
                    .orElseThrow(() -> new RuntimeException("Game history not found"));

            List<String> actions = gameHistory.getPlayerActionIds();

            List<PlayerAction> playerActions = new ArrayList<>();

            for (String MOVEID : actions ){
                System.out.println("Looking for player action with ID: " + MOVEID);
                var playerAction = playerActionRepository.findById(MOVEID);
                if (playerAction.isEmpty()) {
                    System.err.println("Player action not found for ID: " + MOVEID);
                    throw new RuntimeException("Player action not found for ID: " + MOVEID);
                }
                playerActions.add(playerAction.get());
            }

            actions.clear();
            playerActions.sort(Comparator.comparingInt(PlayerAction::getSequenceNumber));
            for (PlayerAction playerAction : playerActions) {
                actions.add(playerAction.getId());
            }



            int totalPlies = playerActions.size();
            int cutoff = Math.max(0, totalPlies - delayPlies);
            BoardPosition enPassantTarget = null;
            BiFunction<Integer, Integer, Piece> getPiece = (x, y) -> delayedboard[y][x];
            TriConsumer<Integer, Integer, Piece> setPiece = (x, y, p) -> delayedboard[y][x] = p;
            BiConsumer<Integer, Integer> clearSquare = (x, y) -> delayedboard[y][x] = null;

            // Apply moves up to cutoff
            for (int i = 0; i < cutoff; i++) {
                PlayerAction a = playerActions.get(i);

                int fromX = a.getFromX();
                int fromY = a.getFromY();
                int toX   = a.getToX();
                int toY   = a.getToY();

                Piece moving = getPiece.apply(fromX, fromY);
                if (moving == null) {
                    // If data is inconsistent, skip defensively
                    continue;
                }

                switch (a.getActionType()) {
                    case NORMAL:
                    case CAPTURE: {
                        // Standard move (capture if target not null)
                        Piece captured = getPiece.apply(toX, toY);
                        setPiece.accept(toX, toY, moving);
                        clearSquare.accept(fromX, fromY);

                        // Pawn double push should have been encoded as DOUBLE_PAWN_PUSH.
                        // Since it's NORMAL here, clear en passant.
                        enPassantTarget = null;
                        moving.setHasMoved(true);
                        break;
                    }

                    case DOUBLE_PAWN_PUSH: {
                        // Move pawn two squares and set en passant target to the passed square
                        setPiece.accept(toX, toY, moving);
                        clearSquare.accept(fromX, fromY);

                        int midY = (fromY + toY) / 2;
                        enPassantTarget = new BoardPosition(midY, toX); // BoardPosition(row, col)
                        moving.setHasMoved(true);
                        break;
                    }

                    case EN_PASSANT: {
                        // Pawn moves to empty square, captured pawn is on (toX, fromY)
                        setPiece.accept(toX, toY, moving);
                        clearSquare.accept(fromX, fromY);

                        // Remove the captured pawn behind the destination rank
                        clearSquare.accept(toX, fromY);

                        enPassantTarget = null;
                        moving.setHasMoved(true);
                        break;
                    }

                    case CASTLE_KINGSIDE: {
                        // King e->g, rook h->f. Need color to pick ranks.
                        boolean isWhite = moving.getColor() == PieceColor.white;
                        int rank = isWhite ? 7 : 0;
                        // king: (4, rank) -> (6, rank)
                        setPiece.accept(6, rank, moving);
                        clearSquare.accept(4, rank);
                        // rook: (7, rank) -> (5, rank)
                        Piece rook = getPiece.apply(7, rank);
                        if (rook != null) {
                            setPiece.accept(5, rank, rook);
                            clearSquare.accept(7, rank);
                            rook.setHasMoved(true);
                        }
                        enPassantTarget = null;
                        moving.setHasMoved(true);
                        break;
                    }

                    case CASTLE_QUEENSIDE: {
                        boolean isWhite = moving.getColor() == PieceColor.white;
                        int rank = isWhite ? 7 : 0;
                        // king: (4, rank) -> (2, rank)
                        setPiece.accept(2, rank, moving);
                        clearSquare.accept(4, rank);
                        // rook: (0, rank) -> (3, rank)
                        Piece rook = getPiece.apply(0, rank);
                        if (rook != null) {
                            setPiece.accept(3, rank, rook);
                            clearSquare.accept(0, rank);
                            rook.setHasMoved(true);
                        }
                        enPassantTarget = null;
                        moving.setHasMoved(true);
                        break;
                    }

                    case PROMOTION: {
                        // You need the promotion piece type; add a field to PlayerAction (e.g., promotionTo).
                        // If you encoded it in abilityUsed, parse it. Example:
                        // PieceType promo = PieceType.valueOf(a.getAbilityUsed()); // ensure it matches enum
                        // Move pawn, then replace with promoted piece with same color.

                        // Fallback (if not available): treat as NORMAL move to keep reconstruction working.
                        setPiece.accept(toX, toY, moving);
                        clearSquare.accept(fromX, fromY);
                        enPassantTarget = null;
                        moving.setHasMoved(true);

                        // Then replace with the promoted piece if available
                        // Piece promoted = new Piece(promo, moving.getColor());
                        // setPiece.accept(toX, toY, promoted);
                        break;
                    }
                }
            }
            // Build the delayed session snapshot
            // 1) Trim move history to the visible prefix
            delayedMoveHistory = actions.subList(0, Math.max(0, actions.size() - delayPlies));
            delayedSession.setMoveHistoryIds(new ArrayList<>(delayedMoveHistory));

            // 2) Assign reconstructed board to session (or inside GameState, depending on your use)
            delayedSession.setBoard(delayedboard);

            // 3) Set turn based on parity (even ply → white to move; odd → black)
            PieceColor turn =
                    (cutoff % 2 == 0)
                            ? PieceColor.white
                            : PieceColor.black;

            // If you want to avoid mutating original GameState, create a lightweight clone
            GameState delayedState =
                    GameState.builder()
                            .gamestateId(originalSession.getGameState().getGamestateId())
                            .gameSessionId(originalSession.getGameId())
                            .currentTurn(turn)
                            .moveCount(cutoff / 2)
                            .isGameOver(false)
                            .isCheck(false)
                            .isCheckmate(false)
                            .canWhiteCastleKingSide(false) // compute properly later if needed
                            .canWhiteCastleQueenSide(false)
                            .canBlackCastleKingSide(false)
                            .canBlackCastleQueenSide(false)
                            .enPassantTarget(null) // or set from local enPassantTarget if you want to expose it
                            .build();

            delayedSession.setGameState(delayedState);

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