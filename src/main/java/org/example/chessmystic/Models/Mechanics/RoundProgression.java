package org.example.chessmystic.Models.Mechanics;

import lombok.*;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoundProgression {
    @Id
    private String id;

    private int baseBoardSize;
    private int sizeIncreasePerBoss;
    private double difficultyMultiplier;
}