package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    
    // Find messages between two users (in both directions)
    List<ChatMessage> findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdOrderByTimestampAsc(
        String senderId, String receiverId, String receiverId2, String senderId2
    );
    
    // Find unread messages for a user
    List<ChatMessage> findByReceiverIdAndIsReadFalseOrderByTimestampDesc(String receiverId);
    
    // Find messages sent by a user
    List<ChatMessage> findBySenderIdOrderByTimestampDesc(String senderId);
    
    // Find messages received by a user
    List<ChatMessage> findByReceiverIdOrderByTimestampDesc(String receiverId);
}
