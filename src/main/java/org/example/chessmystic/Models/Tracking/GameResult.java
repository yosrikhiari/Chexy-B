package org.example.chessmystic.Models.Tracking;

import lombok.*;
import org.example.chessmystic.Models.GameStateandFlow.GameEndReason;
import org.example.chessmystic.Models.chess.PieceColor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameResult {
    private PieceColor winner;
    private String winnerName;
    private int pointsAwarded;
    private GameEndReason gameEndReason;
    private String gameid;
    private String winnerid;
}