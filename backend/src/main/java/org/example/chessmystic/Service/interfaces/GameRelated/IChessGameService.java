package org.example.chessmystic.Service.interfaces.GameRelated;

import org.example.chessmystic.Models.chess.BoardPosition;
import org.example.chessmystic.Models.chess.PieceColor;

public interface IChessGameService {
    boolean validateMove(String gameId, BoardPosition move);
    boolean isCheck(String gameId, PieceColor color);
    boolean isCheckmate(String gameId, PieceColor color);

    boolean isDraw(String gameId, PieceColor color);
}


