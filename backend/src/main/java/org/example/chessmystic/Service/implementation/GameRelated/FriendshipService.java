package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.UserManagement.Friendship;
import org.example.chessmystic.Models.UserManagement.FriendshipStatus;
import org.example.chessmystic.Models.UserManagement.User;
import org.example.chessmystic.Repository.FriendshipRepository;
import org.example.chessmystic.Service.implementation.UserService;
import org.example.chessmystic.Service.interfaces.GameRelated.IFriendshipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class FriendshipService implements IFriendshipService {

    private static final Logger logger = LoggerFactory.getLogger(FriendshipService.class);
    private final FriendshipRepository friendshipRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public FriendshipService(FriendshipRepository friendshipRepository, UserService userService, SimpMessagingTemplate messagingTemplate) {
        this.friendshipRepository = friendshipRepository;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional
    public Friendship sendFriendRequest(String requesterId, String recipientId) {
        logger.info("Sending friend request from {} to {}", requesterId, recipientId);
        if (requesterId.equals(recipientId)) {
            throw new IllegalArgumentException("Cannot send friend request to self");
        }

        userService.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("Requester not found"));
        userService.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        Optional<Friendship> existingFriendship = friendshipRepository.findByUsers(requesterId, recipientId);
        if (existingFriendship.isPresent()) {
            throw new RuntimeException("Friendship or request already exists");
        }

        Friendship friendship = Friendship.builder()
                .requesterId(requesterId)
                .recipientId(recipientId)
                .status(FriendshipStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Friendship savedFriendship = friendshipRepository.save(friendship);
        logger.info("Friend request sent: {}", savedFriendship.getId());

        // Notify both users via WebSocket to refresh friend data
        notifyFriendshipUpdate(requesterId, "REQUEST_SENT", savedFriendship);
        notifyFriendshipUpdate(recipientId, "REQUEST_RECEIVED", savedFriendship);
        return savedFriendship;
    }

    @Override
    @Transactional
    public Friendship acceptFriendRequest(String friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Friendship request not found"));

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new RuntimeException("Request is not in pending state");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        Friendship updatedFriendship = friendshipRepository.save(friendship);
        logger.info("Friend request accepted: {}", friendshipId);
        Friendship persisted = friendshipRepository.save(updatedFriendship);

        // Notify both users
        notifyFriendshipUpdate(persisted.getRequesterId(), "REQUEST_ACCEPTED", persisted);
        notifyFriendshipUpdate(persisted.getRecipientId(), "REQUEST_ACCEPTED", persisted);
        return persisted;
    }

    @Override
    @Transactional
    public Friendship declineFriendRequest(String friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Friendship request not found"));

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new RuntimeException("Request is not in pending state");
        }

        friendshipRepository.delete(friendship);
        logger.info("Friend request declined: {}", friendshipId);
        // Notify both users
        notifyFriendshipUpdate(friendship.getRequesterId(), "REQUEST_DECLINED", friendship);
        notifyFriendshipUpdate(friendship.getRecipientId(), "REQUEST_DECLINED", friendship);
        return friendship;
    }

    @Override
    @Transactional
    public void blockUser(String userId, String userToBlockId) {
        logger.info("Blocking user {} by {}", userToBlockId, userId);
        if (userId.equals(userToBlockId)) {
            throw new IllegalArgumentException("Cannot block self");
        }

        userService.findById(userToBlockId)
                .orElseThrow(() -> new RuntimeException("User to block not found"));

        Optional<Friendship> existingFriendship = friendshipRepository.findByUsers(userId, userToBlockId);
        Friendship friendship;
        if (existingFriendship.isPresent()) {
            friendship = existingFriendship.get();
            friendship.setStatus(FriendshipStatus.BLOCKED);
        } else {
            friendship = Friendship.builder()
                    .requesterId(userId)
                    .recipientId(userToBlockId)
                    .status(FriendshipStatus.BLOCKED)
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        friendshipRepository.save(friendship);
        logger.info("User blocked: {} by {}", userToBlockId, userId);

        // Notify blocker (and optionally blocked user)
        notifyFriendshipUpdate(userId, "USER_BLOCKED", friendship);
        notifyFriendshipUpdate(userToBlockId, "YOU_WERE_BLOCKED", friendship);
    }

    @Override
    @Transactional
    public void unblockUser(String userId, String userToUnblockId) {
        Optional<Friendship> friendship = friendshipRepository.findByUsers(userId, userToUnblockId);
        if (friendship.isPresent() && friendship.get().getStatus() == FriendshipStatus.BLOCKED) {
            friendshipRepository.delete(friendship.get());
            logger.info("User unblocked: {} by {}", userToUnblockId, userId);
            notifyFriendshipUpdate(userId, "USER_UNBLOCKED", friendship.get());
            notifyFriendshipUpdate(userToUnblockId, "YOU_WERE_UNBLOCKED", friendship.get());
        } else {
            throw new RuntimeException("User is not blocked");
        }
    }

    @Override
    public List<List<User>> getFriends(String userId) {
        List<Friendship> friendsIds = friendshipRepository.findByUserIdAndStatus(userId, FriendshipStatus.ACCEPTED);

        List<List<User>> usersThatAreFriends = new ArrayList<>();

        for (Friendship friendship : friendsIds) {
            User user1 = userService.findById(friendship.getRequesterId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            User user2 = userService.findById(friendship.getRecipientId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<User> friends = Arrays.asList(user1, user2);
            usersThatAreFriends.add(friends);
        }

        return usersThatAreFriends;
    }


    @Override
    public List<Friendship> getPendingRequests(String userId) {
        return friendshipRepository.findByRecipientIdAndStatus(userId, FriendshipStatus.PENDING);
    }

    @Override
    public List<Friendship> getSentRequests(String userId) {
        return friendshipRepository.findByRequesterIdAndStatus(userId, FriendshipStatus.PENDING);
    }

    @Override
    public List<Friendship> getBlockedUsers(String userId) {
        return friendshipRepository.findByRequesterIdAndStatus(userId, FriendshipStatus.BLOCKED);
    }

    @Override
    public Optional<Friendship> findFriendshipBetween(String userId1, String userId2) {
        return friendshipRepository.findByUsers(userId1, userId2);
    }

    @Override
    @Transactional
    public void removeFriend(String userId, String friendId) {
        Optional<Friendship> friendship = friendshipRepository.findByUsers(userId, friendId);
        if (friendship.isPresent() && friendship.get().getStatus() == FriendshipStatus.ACCEPTED) {
            friendshipRepository.delete(friendship.get());
            logger.info("Friend removed: {} by {}", friendId, userId);
            // Notify both users
            notifyFriendshipUpdate(userId, "FRIEND_REMOVED", friendship.get());
            notifyFriendshipUpdate(friendId, "FRIEND_REMOVED", friendship.get());
        } else {
            throw new RuntimeException("Friendship does not exist");
        }
    }

    @Override
    public boolean areFriends(String userId1, String userId2) {
        return findFriendshipBetween(userId1, userId2)
                .map(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .orElse(false);
    }

    private void notifyFriendshipUpdate(String userId, String eventType, Friendship friendship) {
        try {
            var payload = new java.util.HashMap<String, Object>();
            payload.put("event", eventType);
            payload.put("friendshipId", friendship.getId());
            payload.put("requesterId", friendship.getRequesterId());
            payload.put("recipientId", friendship.getRecipientId());
            payload.put("status", friendship.getStatus().name());
            payload.put("timestamp", System.currentTimeMillis());
            messagingTemplate.convertAndSend("/queue/friendship/update/" + userId, payload);
        } catch (Exception e) {
            logger.warn("Failed to send friendship update to {}: {}", userId, e.getMessage());
        }
    }
}