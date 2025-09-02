package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.GameStateandFlow.GameState;
import org.example.chessmystic.Models.GameStateandFlow.GameTimers;
import org.example.chessmystic.Models.Interactions.ActionType;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Models.chess.BoardPosition;
import org.example.chessmystic.Models.chess.Piece;
import org.example.chessmystic.Models.chess.PieceColor;
import org.example.chessmystic.Controller.TimerWebSocketController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class GameOrchestrationService {

    private final ChessGameService chessGameService;
    private final PlayerActionService playerActionService;
    private final GameSessionService gameSessionService;
    private final TimerWebSocketController timerWebSocketController;

    @Autowired
    public GameOrchestrationService(ChessGameService chessGameService,
                                    PlayerActionService playerActionService,
                                    GameSessionService gameSessionService,
                                    TimerWebSocketController timerWebSocketController) {
        this.chessGameService = chessGameService;
        this.playerActionService = playerActionService;
        this.gameSessionService = gameSessionService;
        this.timerWebSocketController = timerWebSocketController;
    }

    @Transactional
    public GameState executeMove(String gameId, BoardPosition move) {
        System.out.println("Received move request: " +
                "gameId=" + gameId +
                ", from=[" + move.getRow() + "," + move.getCol() + "]" +
                ", to=[" + move.getTorow() + "," + move.getTocol() + "]");

        var session = gameSessionService.findById(gameId)
                .orElseThrow(() -> {
                    System.err.println("Game session not found: " + gameId);
                    return new IllegalArgumentException("Game session not found");
                });

        if (!isValidPosition(move.getRow(), move.getCol()) ||
                !isValidPosition(move.getTorow(), move.getTocol())) {
            System.err.println("Invalid coordinates detected: " + move);
            throw new IllegalArgumentException("Invalid move coordinates");
        }

        if (!chessGameService.validateMove(gameId, move)) {
            System.err.println("Invalid move detected: " + move);
            System.out.println("Current turn: " + session.getGameState().getCurrentTurn());
            Piece fromPiece = session.getBoard()[move.getRow()][move.getCol()];
            System.out.println("Piece at from position: " + (fromPiece != null ?
                    fromPiece.getColor() + " " + fromPiece.getType() : "None"));
            System.out.println("Target position: " + session.getBoard()[move.getTorow()][move.getTocol()]);
            System.out.println("Current board state:");
            printBoard(session.getBoard());
            throw new IllegalArgumentException("Invalid move");
        }

        GameState gameState = session.getGameState();
        Piece[][] board = session.getBoard();

        int fromRow = move.getRow();
        int fromCol = move.getCol();
        int toRow = move.getTorow();
        int toCol = move.getTocol();

        Piece movingPiece = board[fromRow][fromCol];
        Piece targetPiece = board[toRow][toCol];

        if (movingPiece == null) {
            throw new IllegalArgumentException("No piece at source position");
        }
        if (movingPiece.getColor() != gameState.getCurrentTurn()) {
            System.err.println("Move rejected: Piece color " + movingPiece.getColor() +
                    " does not match current turn " + gameState.getCurrentTurn());
            throw new IllegalArgumentException("Not your turn");
        }

        ActionType actionType = chessGameService.determineActionType(
                movingPiece, targetPiece, fromRow, fromCol, toRow, toCol, board);

        movingPiece.setHasMoved(true);
        board[toRow][toCol] = movingPiece;
        board[fromRow][fromCol] = null;

        chessGameService.handleSpecialMoves(gameState, movingPiece, fromRow, fromCol, toRow, toCol, board);

        String playerId = movingPiece.getColor() == PieceColor.white
                ? session.getWhitePlayer().getUserId()
                : session.getBlackPlayer().getFirst().getUserId();

        playerActionService.recordAction(
                gameId, playerId, actionType, fromRow, fromCol, toRow, toCol,
                session.getGameHistoryId(), session.getRpgGameStateId(), 0, null, 0, false, false);

        chessGameService.updateGameState(gameState, board, session.getGameMode());

        // Update timers
        GameTimers timers = session.getTimers();
        if (gameState.getCurrentTurn() == PieceColor.white) {
            timers.getBlack().setActive(false);
            timers.getWhite().setActive(true);
        } else {
            timers.getWhite().setActive(false);
            timers.getBlack().setActive(true);
        }
        session.setTimers(timers);

        session.setGameState(gameState);
        session.setBoard(board);
        gameSessionService.saveSession(session);
        timerWebSocketController.broadcastTimerUpdate(gameId, session);

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