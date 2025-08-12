package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.chess.BoardPosition;
import org.example.chessmystic.Models.GameStateandFlow.GameState;
import org.example.chessmystic.Models.chess.Piece;
import org.example.chessmystic.Models.Interactions.ActionType;
import org.example.chessmystic.Models.chess.PieceColor;
import org.example.chessmystic.Models.chess.PieceType;
import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Models.UIUX.TieResolutionOption;
import org.example.chessmystic.Repository.GameSessionRepository;
import org.example.chessmystic.Repository.GameStateRepository;
import org.example.chessmystic.Repository.RPGGameStateRepository;
import org.example.chessmystic.Service.interfaces.GameRelated.IChessGameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
public class ChessGameService implements IChessGameService {
    private static final Logger logger = LoggerFactory.getLogger(ChessGameService.class);


    private final GameSessionRepository gameSessionRepository;
    private final TieResolutionOptionService tieResolutionOptionService;

    @Autowired
    public ChessGameService(GameSessionRepository gameSessionRepository,
                            GameStateRepository gameStateRepository,
                            RPGGameStateRepository rpgGameStateRepository,
                            TieResolutionOptionService tieResolutionOptionService) {
        // Remove playerActionService
        this.gameSessionRepository = gameSessionRepository;
        this.tieResolutionOptionService = tieResolutionOptionService;
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
        int toRow = move.getTorow();
        int toCol = move.getTocol();

        if (isValidPosition(fromRow, fromCol) || isValidPosition(toRow, toCol)) {
            return false;
        }

        Piece movingPiece = board[fromRow][fromCol];
        if (movingPiece == null) {
            return false;
        }

        if (movingPiece.getColor() != gameState.getCurrentTurn()) {
            return false;
        }

        // Handle castling first
        if (movingPiece.getType() == PieceType.KING && Math.abs(toCol - fromCol) == 2) {
            if (!validateSpecialMoves(gameState, movingPiece, fromRow, fromCol, toRow, toCol, board)) {
                logger.info("Castling invalid: special move conditions not met");
                return false;
            }
            Piece[][] tempBoard = simulateMove(board, fromRow, fromCol, toRow, toCol);
            if (isKingInCheck(tempBoard, movingPiece.getColor())) {
                logger.info("Castling invalid: king would be in check");
                return false;
            }
            return true;
        }

        // Regular move validation
        if (!isValidPieceMove(movingPiece, fromRow, fromCol, toRow, toCol, board)) {
            logger.info("Invalid piece move for " + movingPiece.getType());
            return false;
        }

        Piece[][] tempBoard = simulateMove(board, fromRow, fromCol, toRow, toCol);
        if (isKingInCheck(tempBoard, movingPiece.getColor())) {
            return false;
        }

        return validateSpecialMoves(gameState, movingPiece, fromRow, fromCol, toRow, toCol, board);
    }

    @Override
    public boolean isCheck(String gameId, PieceColor color) {
        var session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found"));
        Piece[][] board = session.getBoard();
        if (board == null) {
            System.err.println("Board is null for game session: " + gameId);
            return false; // Assume not in check if board is uninitialized
        }
        return isKingInCheck(board, color);
    }

    @Override
    public boolean isCheckmate(String gameId, PieceColor color) {
        var session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found"));

        if (!isKingInCheck(session.getBoard(), color)) {
            return false;
        }

        return hasLegalMoves(session.getBoard(), color);
    }

    @Override
    public boolean isDraw(String gameId, PieceColor color) {
        var session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game session not found"));
        Piece[][] board = session.getBoard();

        // Stalemate: Not in check, but no legal moves
        if (!isKingInCheck(board, color) && hasLegalMoves(board, color)) {
            return true;
        }

        // Insufficient material (simplified check for common cases)
        int whiteMaterial = 0, blackMaterial = 0;
        boolean hasNonKingPiece = false;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece != null) {
                    if (piece.getType() != PieceType.KING) {
                        hasNonKingPiece = true;
                        if (piece.getColor() == PieceColor.white) {
                            whiteMaterial += getPieceValue(piece);
                        } else {
                            blackMaterial += getPieceValue(piece);
                        }
                    }
                }
            }
        }
        return !hasNonKingPiece || (whiteMaterial <= 3 && blackMaterial <= 3); // King vs King or minor piece scenarios
    }

    /**
     * Check for threefold repetition and fifty-move rule
     * @param gameSessionId The game session ID
     * @param currentTurn The current player's turn
     * @return true if the game is a draw due to repetition or fifty-move rule
     */
    private boolean isDrawByRepetitionOrFiftyMove(String gameSessionId, PieceColor currentTurn) {
        // TODO: Implement threefold repetition check using move history
        // TODO: Implement fifty-move rule check (50 moves without capture or pawn advance)
        return false;
    }



    private int getPieceValue(Piece piece) {
        switch (piece.getType()) {
            case QUEEN: return 9;
            case ROOK: return 5;
            case BISHOP:
            case KNIGHT: return 3;
            case PAWN: return 1;
            default: return 0;
        }
    }

    ActionType determineActionType(Piece movingPiece, Piece targetPiece, int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        if (targetPiece != null) return ActionType.CAPTURE;
        if (movingPiece.getType() == PieceType.KING && Math.abs(toCol - fromCol) == 2) {
            return toCol > fromCol ? ActionType.CASTLE_KINGSIDE : ActionType.CASTLE_QUEENSIDE;
        }
        if (movingPiece.getType() == PieceType.PAWN && Math.abs(toCol - fromCol) == 1 && board[toRow][toCol] == null) {
            return ActionType.EN_PASSANT;
        }
        if (movingPiece.getType() == PieceType.PAWN && (toRow == 0 || toRow == 7)) {
            return ActionType.PROMOTION;
        }
        if (movingPiece.getType() == PieceType.PAWN && Math.abs(toRow - fromRow) == 2) {
            return ActionType.DOUBLE_PAWN_PUSH;
        }
        return ActionType.NORMAL;
    }


    void updateGameState(GameState gameState, Piece[][] board, GameMode gameMode) {
        gameState.setMoveCount(gameState.getMoveCount() + 1);
        PieceColor nextTurn = gameState.getCurrentTurn() == PieceColor.white ? PieceColor.black : PieceColor.white;
        gameState.setCurrentTurn(nextTurn);
        gameState.setCheck(isKingInCheck(board, nextTurn));
        gameState.setCheckmate(gameState.isCheck() && hasLegalMoves(board, nextTurn));
        if (gameState.isCheck()) {
            gameState.setCheckedPlayer(nextTurn);
        }
        gameState.setGameOver(gameState.isCheckmate() || isDraw(gameState.getGameSessionId(), nextTurn));
    }

    private boolean hasLegalMoves(Piece[][] board, PieceColor color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getColor() == color) {
                    for (int toRow = 0; toRow < 8; toRow++) {
                        for (int toCol = 0; toCol < 8; toCol++) {
                            if (isValidPieceMove(piece, row, col, toRow, toCol, board)) {
                                Piece[][] tempBoard = simulateMove(board, row, col, toRow, toCol);
                                if (!isKingInCheck(tempBoard, color)) {
                                    return false;
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
        return row < 0 || row >= 8 || col < 0 || col >= 8;
    }

    private boolean isValidPieceMove(Piece piece, int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        Piece targetPiece = board[toRow][toCol];
        if (targetPiece != null && targetPiece.getColor() == piece.getColor()) {
            return false;
        }
        switch (piece.getType()) {
            case KING:
                return Math.abs(toRow - fromRow) <= 1 && Math.abs(toCol - fromCol) <= 1;
            case QUEEN:
                return isValidRookMove(fromRow, fromCol, toRow, toCol, board) || isValidBishopMove(fromRow, fromCol, toRow, toCol, board);
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
        if (fromRow != toRow && fromCol != toCol) return false;
        int stepRow = fromRow == toRow ? 0 : (toRow > fromRow ? 1 : -1);
        int stepCol = fromCol == toCol ? 0 : (toCol > fromCol ? 1 : -1);
        int row = fromRow + stepRow;
        int col = fromCol + stepCol;
        while (row != toRow || col != toCol) {
            if (board[row][col] != null) return false;
            row += stepRow;
            col += stepCol;
        }
        return board[toRow][toCol] == null || board[toRow][toCol].getColor() != board[fromRow][fromCol].getColor();
    }

    private boolean isValidBishopMove(int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        if (Math.abs(toRow - fromRow) != Math.abs(toCol - fromCol)) return false;
        int stepRow = toRow > fromRow ? 1 : -1;
        int stepCol = toCol > fromCol ? 1 : -1;
        int row = fromRow + stepRow;
        int col = fromCol + stepCol;
        while (row != toRow || col != toCol) {
            if (board[row][col] != null) return false;
            row += stepRow;
            col += stepCol;
        }
        return board[toRow][toCol] == null || board[toRow][toCol].getColor() != board[fromRow][fromCol].getColor();
    }

    private boolean isValidPawnMove(Piece piece, int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        int direction = piece.getColor() == PieceColor.white ? -1 : 1;  // White moves up (negative), black moves down (positive)
        int startRow = piece.getColor() == PieceColor.white ? 6 : 1;    // White starts at row 6, black at row 1

        // Forward move
        if (fromCol == toCol && board[toRow][toCol] == null) {
            // Single move forward
            if (toRow == fromRow + direction) return true;
            // Double move from starting position
            if (fromRow == startRow && toRow == fromRow + 2 * direction
                    && board[fromRow + direction][fromCol] == null) {
                return true;
            }
        }

        // Captures
        if (Math.abs(toCol - fromCol) == 1 && toRow == fromRow + direction) {
            // Normal capture
            if (board[toRow][toCol] != null && board[toRow][toCol].getColor() != piece.getColor()) {
                return true;
            }
            // En passant
            if (board[fromRow][toCol] != null
                    && board[fromRow][toCol].isEnPassantTarget()) {
                return true;
            }
        }
        return false;
    }

    private boolean validateSpecialMoves(GameState gameState, Piece piece, int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        if (piece.getType() == PieceType.KING && Math.abs(toCol - fromCol) == 2) {
            boolean isKingSide = toCol > fromCol;
            boolean canCastle = piece.getColor() == PieceColor.white ?
                    (isKingSide ? gameState.isCanWhiteCastleKingSide() : gameState.isCanWhiteCastleQueenSide()) :
                    (isKingSide ? gameState.isCanBlackCastleKingSide() : gameState.isCanBlackCastleQueenSide());

            // Add debug logging here
            System.out.println("Castling validation:");
            System.out.println("Color: " + piece.getColor());
            System.out.println("Side: " + (isKingSide ? "kingside" : "queenside"));
            System.out.println("Can castle flag: " + canCastle);
            System.out.println("King has moved: " + piece.isHasMoved());

            if (!canCastle || piece.isHasMoved()) return false;

            int rookCol = isKingSide ? 7 : 0;
            Piece rook = board[fromRow][rookCol];
            if (rook == null || rook.getType() != PieceType.ROOK || rook.isHasMoved()) {
                System.out.println("Rook validation failed");
                return false;
            }

            // Check path is clear
            int step = isKingSide ? 1 : -1;
            for (int col = fromCol + step; col != rookCol; col += step) {
                if (board[fromRow][col] != null) {
                    System.out.println("Path not clear at " + fromRow + "," + col);
                    return false;
                }
            }

            // Check squares aren't under attack - only the squares the king moves through
            // Don't check the king's starting square (already checked elsewhere)
            for (int col = fromCol; col != toCol + step; col += step) {
                if (col == fromCol) continue; // Skip king's starting square
                if (isSquareUnderAttack(fromRow, col, piece.getColor(), board)) {
                    System.out.println("Square under attack at " + fromRow + "," + col);
                    return false;
                }
            }

            return true;
        }
        return true;
    }

    private boolean isSquareUnderAttack(int row, int col, PieceColor color, Piece[][] board) {
        PieceColor opponentColor = color == PieceColor.white ? PieceColor.black : PieceColor.white;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board[r][c];
                if (piece != null && piece.getColor() == opponentColor) {
                    // For castling checks, we need to make sure we're not considering
                    // the opponent's king as an attacker (since kings can't put each other in check)
                    if (piece.getType() != PieceType.KING &&
                            isValidPieceMove(piece, r, c, row, col, board)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }



    void handleSpecialMoves(GameState gameState, Piece piece, int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        if (piece.getType() == PieceType.KING && Math.abs(toCol - fromCol) == 2) {
            // Castling move
            boolean isKingSide = toCol > fromCol;
            int rookCol = isKingSide ? 7 : 0;
            int rookToCol = isKingSide ? toCol - 1 : toCol + 1;

            // Move the rook
            Piece rook = board[fromRow][rookCol];
            board[fromRow][rookToCol] = rook;
            board[fromRow][rookCol] = null;
            rook.setHasMoved(true);

            // Update castling flags
            if (piece.getColor() == PieceColor.white) {
                gameState.setCanWhiteCastleKingSide(false);
                gameState.setCanWhiteCastleQueenSide(false);
            } else {
                gameState.setCanBlackCastleKingSide(false);
                gameState.setCanBlackCastleQueenSide(false);
            }

            System.out.println("Castling performed - flags updated");
        }
        if (piece.getType() == PieceType.PAWN && Math.abs(toCol - fromCol) == 1 && board[toRow][toCol] == null) {
            int direction = piece.getColor() == PieceColor.white ? 1 : -1;
            board[toRow - direction][toCol] = null;
        }
        if (piece.getType() == PieceType.PAWN && (toRow == 0 || toRow == 7)) {
            piece.setType(PieceType.QUEEN);
        }
        if (piece.getType() == PieceType.PAWN && Math.abs(toRow - fromRow) == 2) {
            piece.setEnPassantTarget(true);
            gameState.setEnPassantTarget(new BoardPosition((fromRow + toRow) / 2, toCol));
        } else {
            gameState.setEnPassantTarget(null);
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    if (board[row][col] != null) board[row][col].setEnPassantTarget(false);
                }
            }
        }
    }

    private boolean isKingInCheck(Piece[][] board, PieceColor color) {
        BoardPosition kingPos = null;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (board[row][col] != null && board[row][col].getType() == PieceType.KING && board[row][col].getColor() == color) {
                    kingPos = new BoardPosition(row, col);
                    break;
                }
            }
            if (kingPos != null) break;
        }
        if (kingPos == null) return false;

        PieceColor opponentColor = color == PieceColor.white ? PieceColor.black : PieceColor.white;
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


    public TieResolutionOption selectTieResolutionOption(GameMode gameMode) {
        if (gameMode == GameMode.SINGLE_PLAYER_RPG || gameMode == GameMode.MULTIPLAYER_RPG) {
            var options = tieResolutionOptionService.getAllOptions();
            if (options.isEmpty()) {
                return null;
            }
            // Weighted random selection
            int totalWeight = options.stream().mapToInt(TieResolutionOption::getWeight).sum();
            Random rand = new Random();
            int randomWeight = rand.nextInt(totalWeight) + 1;
            int cumulativeWeight = 0;
            for (TieResolutionOption option : options) {
                cumulativeWeight += option.getWeight();
                if (randomWeight <= cumulativeWeight) {
                    return option;
                }
            }
        }
        return null;
    }


}