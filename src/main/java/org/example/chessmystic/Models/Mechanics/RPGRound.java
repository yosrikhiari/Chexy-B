package org.example.chessmystic.Models.Mechanics;

import lombok.*;
import org.example.chessmystic.Models.rpg.GameObjective;
import org.springframework.data.annotation.Id;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RPGRound {
    @Id
    private String roundNumber;

    private int boardSize;
    private GameObjective objective;
    private Integer turnLimit;
    private RPGBoss boss;
    private List<Object> rewards;
    private boolean isBossRound = false;
    private int coinsReward;
}