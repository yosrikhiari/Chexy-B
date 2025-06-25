package org.example.chessmystic.Models.Tracking;

import lombok.*;
import org.example.chessmystic.Models.GameStateandFlow.GameEndReason;
import org.example.chessmystic.Models.UIUX.TieResolutionOption;
import org.example.chessmystic.Models.chess.PieceColor;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameResult {
    @Id
    private String gameresultId;


    private PieceColor winner;
    private String winnerName;
    private int pointsAwarded;
    private GameEndReason gameEndReason;
    private String gameid;
    private String winnerid;
    private TieResolutionOption tieResolutionOption;
}