package org.example.chessmystic.Models.rpg;

import lombok.*;
import org.example.chessmystic.Models.chess.BoardPosition;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardEffect {
    private String name;
    private String description;
    private BoardEffectType type;
    private List<BoardPosition> positions;
    private Object effect;
    private boolean isActive;
    private Integer intensity;
}