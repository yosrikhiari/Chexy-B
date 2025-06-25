package org.example.chessmystic.Models.Mechanics;

import lombok.*;
import org.example.chessmystic.Models.rpg.BoardEffect;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "board_configurations")
public class BoardConfiguration {
    @Id
    private String id;
    private int round;
    private int boardSize;
    private List<BoardEffect> effects;
    private int teleportPortals;
    private boolean bossRound;
    private boolean enemyQueenExposed;
}