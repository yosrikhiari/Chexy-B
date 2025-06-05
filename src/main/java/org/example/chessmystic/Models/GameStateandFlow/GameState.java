package org.example.chessmystic.Models.GameStateandFlow;

import lombok.*;
import org.example.chessmystic.Models.chess.BoardPosition;
import org.example.chessmystic.Models.chess.PieceColor;
import org.springframework.data.mongodb.core.index.Indexed;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameState {
    @Indexed
    private String gameSessionId;

    private String userId1;
    private String userId2;
    private boolean isCheck;
    private boolean isCheckmate;
    private PieceColor checkedPlayer;
    private BoardPosition enPassantTarget;

    // FIXED: Add current turn tracking
    private PieceColor currentTurn;
    private int moveCount;
    private boolean canWhiteCastleKingSide;
    private boolean canWhiteCastleQueenSide;
    private boolean canBlackCastleKingSide;
    private boolean canBlackCastleQueenSide;
}
