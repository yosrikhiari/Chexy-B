package org.example.chessmystic.Models.GameStateandFlow;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerTimer {
    private int timeLeft; 
    private boolean active;
}