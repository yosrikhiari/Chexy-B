package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.chess.BoardPosition;
import org.example.chessmystic.Models.GameStateandFlow.GameState;
import org.example.chessmystic.Models.chess.Piece;
import org.example.chessmystic.Models.Interactions.ActionType;
import org.example.chessmystic.Models.chess.PieceColor;
import org.example.chessmystic.Models.chess.PieceType;
import org.example.chessmystic.Repository.GameSessionRepository;
import org.example.chessmystic.Service.interfaces.GameRelated.IChessGameService;
import org.example.chessmystic.Service.interfaces.GameRelated.IPlayerActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChessGameService implements IChessGameService {

    private final IPlayerActionService playerActionService;
    private final GameSessionRepository gameSessionRepository;

    @Autowired
    public ChessGameService(IPlayerActionService playerActionService, GameSessionRepository gameSessionRepository) {
        this.playerActionService = playerActionService;
        this.gameSessionRepository = gameSessionRepository;
    }

    @Override
    @Transactional
    public boolean validateMove(String gameId, BoardPosition move) {
        var session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found"));

        GameState gameState = session.getGameState();
        Piece[][] board = session.getBoard();

        int fromRow = move.getRow();
        int fromCol = move.getCol();
        int toRow = move.getRow(); // Note: BoardPosition should have from/to coordinates
        int toCol = move.getCol(); // Adjust if BoardPosition is modified

        // Validate coordinates
        if (!isValidPosition(fromRow, fromCol) || !isValidPosition(toRow, toCol)) {
            return false;
        }

        Piece movingPiece = board[fromRow][fromCol];
        if (movingPiece == null) {
            return false; // No piece at source position
        }

        // Check if it's the correct player's turn
        if (movingPiece.getColor() != gameState.getCurrentTurn()) {
            return false;
        }

        // Validate piece-specific movement rules
        if (!isValidPieceMove(movingPiece, fromRow, fromCol, toRow, toCol, board)) {
            return false;
        }

        // Check if move puts own king in check
        Piece[][] tempBoard = simulateMove(board, fromRow, fromCol, toRow, toCol);
        if (isKingInCheck(tempBoard, movingPiece.getColor())) {
            return false;
        }

        // Check special moves (castling, en passant, pawn promotion)
        if (!validateSpecialMoves(gameState, movingPiece, fromRow, fromCol, toRow, toCol, board)) {
            return false;
        }

        return true;
    }

    @Override
    @Transactional
    public GameState executeMove(String gameId, BoardPosition move) {
        var session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found"));

        if (!validateMove(gameId, move)) {
            throw new IllegalArgumentException("Invalid move");
        }

        GameState gameState = session.getGameState();
        Piece[][] board = session.getBoard();

        int fromRow = move.getRow();
        int fromCol = move.getCol();
        int toRow = move.getRow(); // Adjust if BoardPosition changes
        int toCol = move.getCol();

        Piece movingPiece = board[fromRow][fromCol];
        Piece targetPiece = board[toRow][toCol];

        // Determine action type
        ActionType actionType = ActionType.NORMAL;
        if (targetPiece != null) {
            actionType = ActionType.CAPTURE;
        } else if (movingPiece.getType() == PieceType.KING && Math.abs(toCol - fromCol) == 2) {
            actionType = toCol > fromCol ? ActionType.CASTLE_KINGSIDE : ActionType.CASTLE_QUEENSIDE;
        } else if (movingPiece.getType() == PieceType.PAWN && Math.abs(toCol - fromCol) == 1 && board[toRow][toCol] == null) {
            actionType = ActionType.EN_PASSANT;
        } else if (movingPiece.getType() == PieceType.PAWN && (toRow == 0 || toRow == 7)) {
            actionType = ActionType.PROMOTION;
        } else if (movingPiece.getType() == PieceType.PAWN && Math.abs(toRow - fromRow) == 2) {
            actionType = ActionType.DOUBLE_PAWN_PUSH;
        }

        // Update board
        movingPiece.setHasMoved(true);
        board[toRow][toCol] = movingPiece;
        board[fromRow][fromCol] = null;

        // Handle special moves (e.g., castling, en passant, promotion)
        handleSpecialMoves(gameState, movingPiece, fromRow, fromCol, toRow, toCol, board);

        // Record the player action
        String playerId = movingPiece.getColor() == PieceColor.WHITE
                ? session.getWhitePlayer().getUserId()
                : session.getBlackPlayer().getUserId();
        playerActionService.recordAction(
                gameId, playerId, actionType, fromRow, fromCol, toRow, toCol,
                session.getGameHistoryId(), null, 0, null, 0, false);

        // Update game state
        gameState.setMoveCount(gameState.getMoveCount() + 1);
        gameState.setCurrentTurn(gameState.getCurrentTurn() == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE);

        // Check for check/checkmate
        PieceColor opponentColor = gameState.getCurrentTurn();
        gameState.setCheck(isKingInCheck(board, opponentColor));
        gameState.setCheckmate(gameState.isCheck() && isCheckmate(gameId, opponentColor));
        if (gameState.isCheck()) {
            gameState.setCheckedPlayer(opponentColor);
        }

        session.setGameState(gameState);
        session.setBoard(board);
        gameSessionRepository.save(session);

        return gameState;
    }

    @Override
    public boolean isCheck(String gameId, PieceColor color) {
        var session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found"));
        return isKingInCheck(session.getBoard(), color);
    }

    @Override
    public boolean isCheckmate(String gameId, PieceColor color) {
        var session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found"));

        if (!isKingInCheck(session.getBoard(), color)) {
            return false;
        }

        // Check if any move can prevent check
        Piece[][] board = session.getBoard();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getColor() == color) {
                    for (int toRow = 0; toRow < 8; toRow++) {
                        for (int toCol = 0; toCol < 8; toCol++) {
                            if (isValidPieceMove(piece, row, col, toRow, toCol, board)) {
                                Piece[][] tempBoard = simulateMove(board, row, col, toRow, toCol);
                                if (!isKingInCheck(tempBoard, color)) {
                                    return false; // Found a move to escape check
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    private boolean isValidPieceMove(Piece piece, int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        // Basic piece movement rules
        switch (piece.getType()) {
            case KING:
                return Math.abs(toRow - fromRow) <= 1 && Math.abs(toCol - fromCol) <= 1;
            case QUEEN:
                return isValidRookMove(fromRow, fromCol, toRow, toCol, board) ||
                        isValidBishopMove(fromRow, fromCol, toRow, toCol, board);
            case ROOK:
                return isValidRookMove(fromRow, fromCol, toRow, toCol, board);
            case BISHOP:
                return isValidBishopMove(fromRow, fromCol, toRow, toCol, board);
            case KNIGHT:
                int rowDiff = Math.abs(toRow - fromRow);
                int colDiff = Math.abs(toCol - fromCol);
                return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
            case PAWN:
                return isValidPawnMove(piece, fromRow, fromCol, toRow, toCol, board);
            default:
                return false;
        }
    }

    private boolean isValidRookMove(int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        if (fromRow != toRow && fromCol != toCol) {
            return false;
        }
        int stepRow = fromRow == toRow ? 0 : (toRow > fromRow ? 1 : -1);
        int stepCol = fromCol == toCol ? 0 : (toCol > fromCol ? 1 : -1);
        int row = fromRow + stepRow;
        int col = fromCol + stepCol;
        while (row != toRow || col != toCol) {
            if (board[row][col] != null) {
                return false; // Path is blocked
            }
            row += stepRow;
            col += stepCol;
        }
        return board[toRow][toCol] == null || board[toRow][toCol].getColor() != board[fromRow][fromCol].getColor();
    }

    private boolean isValidBishopMove(int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        if (Math.abs(toRow - fromRow) != Math.abs(toCol - fromCol)) {
            return false;
        }
        int stepRow = toRow > fromRow ? 1 : -1;
        int stepCol = toCol > fromCol ? 1 : -1;
        int row = fromRow + stepRow;
        int col = fromCol + stepCol;
        while (row != toRow && col != toCol) {
            if (board[row][col] != null) {
                return false; // Path is blocked
            }
            row += stepRow;
            col += stepCol;
        }
        return board[toRow][toCol] == null || board[toRow][toCol].getColor() != board[fromRow][fromCol].getColor();
    }

    private boolean isValidPawnMove(Piece piece, int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        int direction = piece.getColor() == PieceColor.WHITE ? 1 : -1;
        int startRow = piece.getColor() == PieceColor.WHITE ? 1 : 6;

        // Move forward
        if (fromCol == toCol && board[toRow][toCol] == null) {
            if (toRow == fromRow + direction) {
                return true;
            }
            if (fromRow == startRow && toRow == fromRow + 2 * direction && board[fromRow + direction][fromCol] == null) {
                return true;
            }
        }
        // Capture
        if (Math.abs(toCol - fromCol) == 1 && toRow == fromRow + direction && board[toRow][toCol] != null &&
                board[toRow][toCol].getColor() != piece.getColor()) {
            return true;
        }
        // En passant (simplified)
        if (Math.abs(toCol - fromCol) == 1 && toRow == fromRow + direction && board[toRow - direction][toCol] != null &&
                board[toRow - direction][toCol].isEnPassantTarget()) {
            return true;
        }
        return false;
    }

    private boolean validateSpecialMoves(GameState gameState, Piece piece, int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        // Handle castling
        if (piece.getType() == PieceType.KING && Math.abs(toCol - fromCol) == 2) {
            boolean isKingSide = toCol > fromCol;
            boolean canCastle = piece.getColor() == PieceColor.WHITE ?
                    (isKingSide ? gameState.isCanWhiteCastleKingSide() : gameState.isCanWhiteCastleQueenSide()) :
                    (isKingSide ? gameState.isCanBlackCastleKingSide() : gameState.isCanBlackCastleQueenSide());
            if (!canCastle || piece.isHasMoved()) {
                return false;
            }
            // Check rook availability and path clearance
            int rookCol = isKingSide ? 7 : 0;
            Piece rook = board[fromRow][rookCol];
            if (rook == null || rook.getType() != PieceType.ROOK || rook.isHasMoved()) {
                return false;
            }
            // Check path clearance
            int step = isKingSide ? 1 : -1;
            for (int col = fromCol + step; col != rookCol; col += step) {
                if (board[fromRow][col] != null) {
                    return false;
                }
            }
            return true;
        }
        return true; // Other special moves (en passant, promotion) handled in executeMove
    }

    private void handleSpecialMoves(GameState gameState, Piece piece, int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        // Handle castling
        if (piece.getType() == PieceType.KING && Math.abs(toCol - fromCol) == 2) {
            boolean isKingSide = toCol > fromCol;
            int rookCol = isKingSide ? 7 : 0;
            int rookToCol = isKingSide ? toCol - 1 : toCol + 1;
            Piece rook = board[fromRow][rookCol];
            board[fromRow][rookToCol] = rook;
            board[fromRow][rookCol] = null;
            // Update castling availability
            if (piece.getColor() == PieceColor.WHITE) {
                gameState.setCanWhiteCastleKingSide(false);
                gameState.setCanWhiteCastleQueenSide(false);
            } else {
                gameState.setCanBlackCastleKingSide(false);
                gameState.setCanBlackCastleQueenSide(false);
            }
        }
        // Handle en passant
        if (piece.getType() == PieceType.PAWN && Math.abs(toCol - fromCol) == 1 && board[toRow][toCol] == null) {
            int direction = piece.getColor() == PieceColor.WHITE ? 1 : -1;
            board[toRow - direction][toCol] = null; // Remove captured pawn
        }
        // Handle pawn promotion (simplified to always promote to queen)
        if (piece.getType() == PieceType.PAWN && (toRow == 0 || toRow == 7)) {
            piece.setType(PieceType.QUEEN);
        }
        // Update en passant target
        if (piece.getType() == PieceType.PAWN && Math.abs(toRow - fromRow) == 2) {
            piece.setEnPassantTarget(true);
            gameState.setEnPassantTarget(new BoardPosition((fromRow + toRow) / 2, toCol));
        } else {
            gameState.setEnPassantTarget(null);
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    if (board[row][col] != null) {
                        board[row][col].setEnPassantTarget(false);
                    }
                }
            }
        }
    }

    private boolean isKingInCheck(Piece[][] board, PieceColor color) {
        // Find king's position
        BoardPosition kingPos = null;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (board[row][col] != null && board[row][col].getType() == PieceType.KING &&
                        board[row][col].getColor() == color) {
                    kingPos = new BoardPosition(row, col);
                    break;
                }
            }
            if (kingPos != null) break;
        }
        if (kingPos == null) return false; // No king (shouldn't happen in valid game)

        // Check if any opponent piece can attack the king
        PieceColor opponentColor = color == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getColor() == opponentColor) {
                    if (isValidPieceMove(piece, row, col, kingPos.getRow(), kingPos.getCol(), board)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Piece[][] simulateMove(Piece[][] board, int fromRow, int fromCol, int toRow, int toCol) {
        Piece[][] tempBoard = new Piece[8][8];
        for (int i = 0; i < 8; i++) {
            tempBoard[i] = board[i].clone();
        }
        Piece movingPiece = tempBoard[fromRow][fromCol];
        tempBoard[toRow][toCol] = movingPiece;
        tempBoard[fromRow][fromCol] = null;
        return tempBoard;
    }
}