package org.example.chessmystic.Service.interfaces;

import org.example.chessmystic.Models.APIContacts.LoginDTO.LoginRequestDTO;
import org.example.chessmystic.Models.APIContacts.LoginDTO.LoginResponseDTO;
import org.example.chessmystic.Models.APIContacts.RegistreDTO.RegisterRequestDTO;
import org.example.chessmystic.Models.APIContacts.RegistreDTO.RegisterResponseDTO;
import org.example.chessmystic.Models.APIContacts.UserDTO.UserUpdateDTO;
import org.example.chessmystic.Models.UserManagement.User;
import org.example.chessmystic.Models.Stats.PlayerProfile;

import java.util.List;
import java.util.Optional;

public interface IUserService {
    RegisterResponseDTO registerUser(RegisterRequestDTO dto);
    LoginResponseDTO loginUser(LoginRequestDTO dto);
    User createUser(User user);
    Optional<User> findById(String id);
    Optional<User> findByKeycloakId(String keycloakId);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    List<User> findAllActiveUsers();
    UserUpdateDTO updateCurrentUserauthheader(String authHeader, UserUpdateDTO updateDTO);
    UserUpdateDTO updateCurrentUser(String id, UserUpdateDTO updateDTO); // Added overloaded method
    void deleteUser(String id);
    void deactivateUser(String id);
    List<User> getLeaderboard(int limit);
    void updateUserPoints(String userId, int points);
    PlayerProfile getOrCreatePlayerProfile(String userId);

    Optional<User> findByUserId(String Id);
}