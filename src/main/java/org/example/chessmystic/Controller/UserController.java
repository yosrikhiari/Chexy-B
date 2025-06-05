package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.APIContacts.UserDTO.UserUpdateDTO;
import org.example.chessmystic.Models.UserManagement.User;
import org.example.chessmystic.Service.interfaces.IUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/user")
public class UserController {

    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(@RequestParam String keycloakId) {
        try {
            Optional<User> user = userService.findByKeycloakId(keycloakId);
            return user.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body((User) Map.of("error", "User not found")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve user", "message", "An unexpected error occurred"));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PatchMapping("/update")
    public ResponseEntity<?> updateCurrentUser(@RequestHeader("Authorization") String authHeader,
                                               @RequestBody UserUpdateDTO dto) {
        try {
            UserUpdateDTO updatedUser = userService.updateCurrentUserauthheader(authHeader, dto);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update user", "message", "An unexpected error occurred"));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PatchMapping("/points")
    public ResponseEntity<?> updateUserPoints(@RequestParam String userId, @RequestParam int points) {
        try {
            userService.updateUserPoints(userId, points);
            return ResponseEntity.ok(Map.of("message", "Points updated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update points", "message", "An unexpected error occurred"));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/leaderboard")
    public ResponseEntity<List<User>> getLeaderboard(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<User> leaderboard = userService.getLeaderboard(limit);
            return ResponseEntity.ok(leaderboard);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PatchMapping("/deactivate")
    public ResponseEntity<?> deactivateUser(@RequestParam String id) {
        try {
            userService.deactivateUser(id);
            return ResponseEntity.ok(Map.of("message", "User deactivated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to deactivate user", "message", "An unexpected error occurred"));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @DeleteMapping
    public ResponseEntity<?> deleteUser(@RequestParam String id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete user", "message", "An unexpected error occurred"));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PatchMapping("/update/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable String userId, @RequestBody UserUpdateDTO dto) {
        try {
            UserUpdateDTO updatedUser = userService.updateCurrentUser(userId, dto);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update user", "message", "An unexpected error occurred"));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/all-active")
    public ResponseEntity<List<User>> getAllActiveUsers() {
        try {
            List<User> users = userService.findAllActiveUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/username/{username}")
    public ResponseEntity<?> findByUsername(@PathVariable String username) {
        try {
            Optional<User> user = userService.findByUsername(username);
            return user.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body((User) Map.of("error", "User not found")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to find user", "message", "An unexpected error occurred"));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/email/{email}")
    public ResponseEntity<?> findByEmail(@PathVariable String email) {
        try {
            Optional<User> user = userService.findByEmail(email);
            return user.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body((User) Map.of("error", "User not found")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to find user", "message", "An unexpected error occurred"));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create user", "message", "An unexpected error occurred"));
        }
    }
}