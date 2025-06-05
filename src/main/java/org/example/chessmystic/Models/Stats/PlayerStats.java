package org.example.chessmystic.Models.Stats;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerStats {
    private  String playerId;
    private String name;
    private int points;
}