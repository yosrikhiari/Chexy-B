package org.example.chessmystic.Models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "spectator_chat_messages")
public class SpectatorChatMessage {
    private String senderId;
    private String senderName;
    private String gameId;
    private String message;
    private String timestamp;
    private boolean isSpectatorMessage;
}
