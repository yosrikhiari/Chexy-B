package org.example.chessmystic.Models.UserManagement;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "friendships")
public class Friendship {
    @Id
    private String id;
    private String requesterId;
    private String recipientId;
    private FriendshipStatus status;
    private LocalDateTime createdAt;
}
