package org.example.chessmystic.Models.GameStateandFlow;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameTimers {
    private PlayerTimer white;
    private PlayerTimer black;
    private int defaultTime;

    // Optional server timestamp (ms since epoch) indicating when these timers are valid
    private Long serverTimeMs;




}