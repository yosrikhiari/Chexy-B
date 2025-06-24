package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.UserManagement.Friendship;
import org.example.chessmystic.Models.UserManagement.FriendshipStatus;
import org.example.chessmystic.Repository.FriendshipRepository;
import org.example.chessmystic.Service.implementation.UserService;
import org.example.chessmystic.Service.interfaces.GameRelated.IFriendshipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FriendshipService implements IFriendshipService {

    private static final Logger logger = LoggerFactory.getLogger(FriendshipService.class);
    private final FriendshipRepository friendshipRepository;
    private final UserService userService;

    @Autowired
    public FriendshipService(FriendshipRepository friendshipRepository, UserService userService) {
        this.friendshipRepository = friendshipRepository;
        this.userService = userService;
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
        return friendshipRepository.save(updatedFriendship);
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
    }

    @Override
    @Transactional
    public void unblockUser(String userId, String userToUnblockId) {
        Optional<Friendship> friendship = friendshipRepository.findByUsers(userId, userToUnblockId);
        if (friendship.isPresent() && friendship.get().getStatus() == FriendshipStatus.BLOCKED) {
            friendshipRepository.delete(friendship.get());
            logger.info("User unblocked: {} by {}", userToUnblockId, userId);
        } else {
            throw new RuntimeException("User is not blocked");
        }
    }

    @Override
    public List<Friendship> getFriends(String userId) {
        return friendshipRepository.findByUserIdAndStatus(userId, FriendshipStatus.ACCEPTED);
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
}