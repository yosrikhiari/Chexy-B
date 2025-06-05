package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.UserManagement.Friendship;
import org.example.chessmystic.Service.interfaces.GameRelated.IFriendshipService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/friendship")
public class FriendshipController {

    private final IFriendshipService friendshipService;

    public FriendshipController(IFriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/request")
    public ResponseEntity<?> sendFriendRequest(@RequestParam String requesterId, @RequestParam String recipientId) {
        try {
            Friendship friendship = friendshipService.sendFriendRequest(requesterId, recipientId);
            return ResponseEntity.status(HttpStatus.CREATED).body(friendship);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send friend request", "message", "An unexpected error occurred"));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PutMapping("/accept/{friendshipId}")
    public ResponseEntity<?> acceptFriendRequest(@PathVariable String friendshipId) {
        try {
            Friendship friendship = friendshipService.acceptFriendRequest(friendshipId);
            return ResponseEntity.ok(friendship);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to accept friend request", "message", "An unexpected error occurred"));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @DeleteMapping("/decline/{friendshipId}")
    public ResponseEntity<?> declineFriendRequest(@PathVariable String friendshipId) {
        try {
            Friendship friendship = friendshipService.declineFriendRequest(friendshipId);
            return ResponseEntity.ok(friendship);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to decline friend request", "message", "An unexpected error occurred"));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/block")
    public ResponseEntity<?> blockUser(@RequestParam String userId, @RequestParam String userToBlockId) {
        try {
            friendshipService.blockUser(userId, userToBlockId);
            return ResponseEntity.ok(Map.of("message", "User blocked successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to block user", "message", "An unexpected error occurred"));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/unblock")
    public ResponseEntity<?> unblockUser(@RequestParam String userId, @RequestParam String userToUnblockId) {
        try {
            friendshipService.unblockUser(userId, userToUnblockId);
            return ResponseEntity.ok(Map.of("message", "User unblocked successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to unblock user", "message", "An unexpected error occurred"));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/friends/{userId}")
    public ResponseEntity<List<Friendship>> getFriends(@PathVariable String userId) {
        try {
            List<Friendship> friends = friendshipService.getFriends(userId);
            return ResponseEntity.ok(friends);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/pending/{userId}")
    public ResponseEntity<List<Friendship>> getPendingRequests(@PathVariable String userId) {
        try {
            List<Friendship> pendingRequests = friendshipService.getPendingRequests(userId);
            return ResponseEntity.ok(pendingRequests);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/sent/{userId}")
    public ResponseEntity<List<Friendship>> getSentRequests(@PathVariable String userId) {
        try {
            List<Friendship> sentRequests = friendshipService.getSentRequests(userId);
            return ResponseEntity.ok(sentRequests);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/blocked/{userId}")
    public ResponseEntity<List<Friendship>> getBlockedUsers(@PathVariable String userId) {
        try {
            List<Friendship> blockedUsers = friendshipService.getBlockedUsers(userId);
            return ResponseEntity.ok(blockedUsers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @DeleteMapping("/remove/{userId}/{friendId}")
    public ResponseEntity<?> removeFriend(@PathVariable String userId, @PathVariable String friendId) {
        try {
            friendshipService.removeFriend(userId, friendId);
            return ResponseEntity.ok(Map.of("message", "Friend removed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to remove friend", "message", "An unexpected error occurred"));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/are-friends/{userId1}/{userId2}")
    public ResponseEntity<Boolean> areFriends(@PathVariable String userId1, @PathVariable String userId2) {
        try {
            boolean areFriends = friendshipService.areFriends(userId1, userId2);
            return ResponseEntity.ok(areFriends);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }
}