package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.GameStateandFlow.GameState;
import org.example.chessmystic.Models.GameStateandFlow.GameTimers;
import org.example.chessmystic.Models.Interactions.ActionType;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Models.chess.BoardPosition;
import org.example.chessmystic.Models.chess.Piece;
import org.example.chessmystic.Models.chess.PieceColor;
import org.example.chessmystic.Controller.TimerWebSocketController;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.example.chessmystic.Repository.GameSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class GameOrchestrationService {


    private final ChessGameService chessGameService;
    private final PlayerActionService playerActionService;
    private final GameSessionService gameSessionService;
    private final TimerWebSocketController timerWebSocketController;
    private final GameSessionRepository gameSessionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public GameOrchestrationService(ChessGameService chessGameService,
                                    PlayerActionService playerActionService,
                                    GameSessionService gameSessionService,
                                    TimerWebSocketController timerWebSocketController, GameSessionRepository gameSessionRepository, SimpMessagingTemplate messagingTemplate) {
        this.chessGameService = chessGameService;
        this.playerActionService = playerActionService;
        this.gameSessionService = gameSessionService;
        this.timerWebSocketController = timerWebSocketController;
        this.gameSessionRepository = gameSessionRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public GameState executeMove(String gameId, BoardPosition move) {
        System.out.println("Received move request: " +
                "gameId=" + gameId +
                ", from=[" + move.getRow() + "," + move.getCol() + "]" +
                ", to=[" + move.getTorow() + "," + move.getTocol() + "]");

        // Add more detailed debugging
        System.out.println("Searching for game session with ID: " + gameId);
        var session = gameSessionService.findById(gameId);
        
        if (session.isEmpty()) {
            System.err.println("Game session not found for ID: " + gameId);
            System.err.println("Available game sessions in database:");
            try {
                // Try to get all active games for debugging
                var allSessions = gameSessionRepository.findAll();
                System.err.println("Total game sessions in database: " + allSessions.size());
                allSessions.forEach(s -> System.err.println("  - Game ID: " + s.getGameId() + ", Status: " + s.getStatus() + ", Created: " + s.getCreatedAt()));
                
                // Also try to find by partial match
                System.err.println("Searching for similar game IDs...");
                allSessions.stream()
                    .filter(s -> s.getGameId().contains(gameId.substring(0, 8)))
                    .forEach(s -> System.err.println("  - Similar Game ID: " + s.getGameId()));
                    
            } catch (Exception e) {
                System.err.println("Could not retrieve game sessions for debugging: " + e.getMessage());
                e.printStackTrace();
            }
            throw new IllegalArgumentException("Game session not found for ID: " + gameId);
        }
        
        var gameSession = session.get();
        System.out.println("Game session retrieved successfully: " + gameSession.getGameId());

        if (!isValidPosition(move.getRow(), move.getCol()) ||
                !isValidPosition(move.getTorow(), move.getTocol())) {
            System.err.println("Invalid coordinates detected: " + move);
            throw new IllegalArgumentException("Invalid move coordinates");
        }

        System.out.println("About to validate move...");
        if (!chessGameService.validateMove(gameId, move)) {
            System.err.println("Invalid move detected: " + move);
            System.out.println("Current turn: " + gameSession.getGameState().getCurrentTurn());
            Piece fromPiece = gameSession.getBoard()[move.getRow()][move.getCol()];
            System.out.println("Piece at from position: " + (fromPiece != null ?
                    fromPiece.getColor() + " " + fromPiece.getType() : "None"));
            System.out.println("Target position: " + gameSession.getBoard()[move.getTorow()][move.getTocol()]);
            System.out.println("Current board state:");
            printBoard(gameSession.getBoard());
            throw new IllegalArgumentException("Invalid move");
        }
        System.out.println("Move validation passed");

        System.out.println("Getting game state and board...");
        GameState gameState = gameSession.getGameState();
        if (gameState == null) {
            System.err.println("GameState is null!");
            throw new IllegalArgumentException("GameState is null");
        }
        
        Piece[][] board = gameSession.getBoard();
        if (board == null) {
            System.err.println("Board is null!");
            throw new IllegalArgumentException("Board is null");
        }
        System.out.println("Game state and board retrieved successfully");

        int fromRow = move.getRow();
        int fromCol = move.getCol();
        int toRow = move.getTorow();
        int toCol = move.getTocol();

        System.out.println("Getting pieces at positions...");
        Piece movingPiece = board[fromRow][fromCol];
        Piece targetPiece = board[toRow][toCol];

        if (movingPiece == null) {
            System.err.println("No piece at source position [" + fromRow + "," + fromCol + "]");
            throw new IllegalArgumentException("No piece at source position");
        }
        if (movingPiece.getColor() != gameState.getCurrentTurn()) {
            System.err.println("Move rejected: Piece color " + movingPiece.getColor() +
                    " does not match current turn " + gameState.getCurrentTurn());
            throw new IllegalArgumentException("Not your turn");
        }
        System.out.println("Piece validation passed");

        System.out.println("Determining action type...");
        ActionType actionType = chessGameService.determineActionType(
                movingPiece, targetPiece, fromRow, fromCol, toRow, toCol, board);
        System.out.println("Action type determined: " + actionType);

        System.out.println("Executing move on board...");
        movingPiece.setHasMoved(true);
        board[toRow][toCol] = movingPiece;
        board[fromRow][fromCol] = null;

        System.out.println("Handling special moves...");
        chessGameService.handleSpecialMoves(gameState, movingPiece, fromRow, fromCol, toRow, toCol, board);

        System.out.println("Getting player ID...");
        String playerId = movingPiece.getColor() == PieceColor.white
                ? gameSession.getWhitePlayer().getUserId()
                : gameSession.getBlackPlayer().getFirst().getUserId();
        System.out.println("Player ID: " + playerId);

        System.out.println("Recording player action...");
        playerActionService.recordAction(
                gameId, playerId, actionType, fromRow, fromCol, toRow, toCol,
                gameSession.getGameHistoryId(), gameSession.getRpgGameStateId(), 0, null, 0, false, false);
        System.out.println("Player action recorded successfully");

        System.out.println("Updating game state...");
        chessGameService.updateGameState(gameState, board, gameSession.getGameMode());
        System.out.println("Game state updated successfully");

        // Update timers
        System.out.println("Updating timers...");
        GameTimers timers = gameSession.getTimers();
        if (timers == null) {
            System.err.println("Timers is null!");
            throw new IllegalArgumentException("Timers is null");
        }
        if (gameState.getCurrentTurn() == PieceColor.white) {
            timers.getBlack().setActive(false);
            timers.getWhite().setActive(true);
        } else {
            timers.getWhite().setActive(false);
            timers.getBlack().setActive(true);
        }
        gameSession.setTimers(timers);
        System.out.println("Timers updated successfully");

        System.out.println("Saving game session...");
        gameSession.setGameState(gameState);
        gameSession.setBoard(board);
        gameSessionService.saveSession(gameSession);
        System.out.println("Game session saved successfully");

        System.out.println("Cleaning up spectator sessions...");
        if (gameSessionRepository.findById("SpecSession-"+gameSession.getGameId()).isPresent()) {
            gameSessionRepository.removeByGameId("SpecSession-"+gameSession.getGameId());
        }

        System.out.println("Creating delayed game session...");
        GameSession delayedSession = null;
        try {
            delayedSession = RealtimeService.createDelayedGameSession(gameSession.getGameId(), 2);
            if (delayedSession != null) {
                gameSessionService.saveSession(delayedSession);
                System.out.println("Delayed game session created successfully");
            } else {
                System.out.println("No delayed game session needed (not enough moves)");
            }
        } catch (Exception e) {
            System.err.println("Failed to create delayed game session: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Broadcasting timer update...");
        timerWebSocketController.broadcastTimerUpdate(gameId, gameSession);

        System.out.println("Move execution completed successfully");

        try {
            if (delayedSession != null) {
                messagingTemplate.convertAndSend("/topic/spectator-game-state/" + gameId, delayedSession.getGameState());
                messagingTemplate.convertAndSend("/topic/timer-updates/" + gameId, delayedSession.getTimers());
            }
        } catch (Exception e) {
            System.err.println("Delayed Session publish failed: " + e.getMessage());
        }
        return gameState;
    }

    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }




    private void printBoard(Piece[][] board) {
        System.out.println("Board state:");
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece != null) {
                    char color = piece.getColor() == PieceColor.white ? 'w' : 'b';
                    char type = switch (piece.getType().name().toLowerCase()) {
                        case "king" -> 'K';
                        case "queen" -> 'Q';
                        case "rook" -> 'R';
                        case "bishop" -> 'B';
                        case "knight" -> 'N';
                        case "pawn" -> 'P';
                        default -> '?';
                    };
                    System.out.print(color + type + " ");
                } else {
                    System.out.print("-- ");
                }
            }
            System.out.println(" " + (8 - row));
        }
        System.out.println("  a  b  c  d  e  f  g  h");
    }
}