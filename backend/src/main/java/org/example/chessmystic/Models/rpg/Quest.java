package org.example.chessmystic.Models.rpg;

import lombok.*;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quest {
    @Id
    private String id;

    private QuestType type;
    private String assignedToPlayerId;
    private String description;
    private int target;
    private int progress;
    private boolean completed;
    private int coinsReward; // MVP reward type
}




