package org.example.chessmystic.Service.interfaces.GameRelated;

import org.example.chessmystic.Models.UserManagement.Friendship;

import java.util.List;
import java.util.Optional;

public interface IFriendshipService {
    Friendship sendFriendRequest(String requesterId, String recipientId);
    Friendship acceptFriendRequest(String friendshipId);
    Friendship declineFriendRequest(String friendshipId);
    void blockUser(String userId, String userToBlockId);
    void unblockUser(String userId, String userToUnblockId);
    List<Friendship> getFriends(String userId);
    List<Friendship> getPendingRequests(String userId);
    List<Friendship> getSentRequests(String userId);
    List<Friendship> getBlockedUsers(String userId);
    Optional<Friendship> findFriendshipBetween(String userId1, String userId2);
    void removeFriend(String userId, String friendId);
    boolean areFriends(String userId1, String userId2);

}
