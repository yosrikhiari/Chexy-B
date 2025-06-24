package org.example.chessmystic.Models.Tounaments;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "tournaments")
public class Tournament {
    @Id
    private String id;
    private String name;
    private LocalDateTime startTime;
    private List<String> participantIds; // Links to User.id
    private List<String> gameSessionIds; // Links to GameSession.gameId
}
