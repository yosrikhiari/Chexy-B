package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.UserManagement.Friendship;
import org.example.chessmystic.Models.UserManagement.FriendshipStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends MongoRepository<Friendship, String> {
    List<Friendship> findByRequesterIdAndStatus(String requesterId, FriendshipStatus status);
    List<Friendship> findByRecipientIdAndStatus(String recipientId, FriendshipStatus status);

    @Query("{'$or': [{'requesterId': ?0}, {'recipientId': ?0}], 'status': ?1}")
    List<Friendship> findByUserIdAndStatus(String userId, FriendshipStatus status);

    @Query("{'$or': [{'requesterId': ?0, 'recipientId': ?1}, {'requesterId': ?1, 'recipientId': ?0}]}")
    Optional<Friendship> findByUsers(String userId1, String userId2);
}
