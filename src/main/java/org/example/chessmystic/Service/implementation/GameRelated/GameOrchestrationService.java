package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.GameStateandFlow.GameState;
import org.example.chessmystic.Models.Interactions.ActionType;
import org.example.chessmystic.Models.chess.BoardPosition;
import org.example.chessmystic.Models.chess.Piece;
import org.example.chessmystic.Models.chess.PieceColor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameOrchestrationService {

    private final ChessGameService chessGameService;
    private final PlayerActionService playerActionService;
    private final GameSessionService gameSessionService;

    @Autowired
    public GameOrchestrationService(ChessGameService chessGameService,
                                    PlayerActionService playerActionService,
                                    GameSessionService gameSessionService) {
        this.chessGameService = chessGameService;
        this.playerActionService = playerActionService;
        this.gameSessionService = gameSessionService;
    }

    @Transactional
    public GameState executeMove(String gameId, BoardPosition move) {
        // Move the executeMove logic here
        var session = gameSessionService.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found"));

        if (!chessGameService.validateMove(gameId, move)) {
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

        ActionType actionType = chessGameService.determineActionType(movingPiece, targetPiece, fromRow, fromCol, toRow, toCol, board);

        // Update board
        movingPiece.setHasMoved(true);
        board[toRow][toCol] = movingPiece;
        board[fromRow][fromCol] = null;

        chessGameService.handleSpecialMoves(gameState, movingPiece, fromRow, fromCol, toRow, toCol, board);

        String playerId = movingPiece.getColor() == PieceColor.WHITE
                ? session.getWhitePlayer().getUserId()
                : session.getBlackPlayer().get(0).getUserId();

        playerActionService.recordAction(
                gameId, playerId, actionType, fromRow, fromCol, toRow, toCol,
                session.getGameHistoryId(), session.getRpgGameStateId(), 0, null, 0, false, false);

        chessGameService.updateGameState(gameState, board, session.getGameMode());

        session.setGameState(gameState);
        session.setBoard(board);
        gameSessionService.saveSession(session);

        return gameState;
    }
}