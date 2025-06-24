package org.example.chessmystic.Service.implementation;

import org.example.chessmystic.Models.APIContacts.UserDTO.UserResponseDTO;
import org.example.chessmystic.Models.UserManagement.User;
import org.example.chessmystic.Models.APIContacts.UserDTO.UserUpdateDTO;
import org.example.chessmystic.Models.Stats.PlayerProfile;
import org.example.chessmystic.Models.Stats.PlayerGameStats;
import org.example.chessmystic.Models.APIContacts.LoginDTO.LoginRequestDTO;
import org.example.chessmystic.Models.APIContacts.LoginDTO.LoginResponseDTO;
import org.example.chessmystic.Models.APIContacts.RegistreDTO.RegisterRequestDTO;
import org.example.chessmystic.Models.APIContacts.RegistreDTO.RegisterResponseDTO;
import org.example.chessmystic.Models.UserManagement.Role;
import org.example.chessmystic.Exceptions.UserAlreadyExistsException;
import org.example.chessmystic.Exceptions.AuthenticationException;
import org.example.chessmystic.Helper.JwtDecoder;
import org.example.chessmystic.Repository.UserRepository;
import org.example.chessmystic.Repository.PlayerProfileRepository;
import org.example.chessmystic.Service.interfaces.IUserService;
import org.example.chessmystic.Service.interfaces.IAuthService;
import org.example.chessmystic.Service.interfaces.IKeycloakUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final IKeycloakUserService keycloakUserService;
    private final IAuthService authService;

    @Autowired
    public UserService(UserRepository userRepository, PlayerProfileRepository playerProfileRepository,
                       IKeycloakUserService keycloakUserService, IAuthService authService) {
        this.userRepository = userRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.keycloakUserService = keycloakUserService;
        this.authService = authService;
    }

    @Override
    @Transactional
    public RegisterResponseDTO registerUser(RegisterRequestDTO dto) {
        // Check for duplicate username or email
        userRepository.findByUsername(dto.getUsername())
                .ifPresent(u -> {
                    throw new UserAlreadyExistsException("Username already exists");
                });
        userRepository.findByEmailAddress(dto.getEmail())
                .ifPresent(u -> {
                    throw new UserAlreadyExistsException("Email already exists");
                });

        // Create user in Keycloak
        String keycloakId = keycloakUserService.createUser(
                dto.getUsername(), dto.getFirstname(), dto.getLastname(),
                dto.getEmail(), dto.getPassword());

        // Create User entity
        User user = User.builder()
                .username(dto.getUsername())
                .emailAddress(dto.getEmail())
                .firstName(dto.getFirstname())
                .lastName(dto.getLastname())
                .keycloakId(keycloakId)
                .points(0)
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        // Create PlayerProfile and link it
        PlayerProfile profile = getOrCreatePlayerProfile(savedUser.getId());
        savedUser.setPlayerProfileId(profile.getId());
        userRepository.save(savedUser);

        return RegisterResponseDTO.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmailAddress())
                .role(savedUser.getRole())
                .message("User registered successfully")
                .build();
    }

    @Override
    public LoginResponseDTO loginUser(LoginRequestDTO dto) {
        User user = userRepository.findByEmailAddress(dto.getEmailAddress())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        String token = authService.login(dto.getEmailAddress(), dto.getPassword());
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return LoginResponseDTO.builder()
                .token(token)
                .user(UserResponseDTO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmailAddress())
                        .role(user.getRole())
                        .build())
                .message("Login successful")
                .build();
    }

    @Override
    @Transactional
    public UserUpdateDTO updateCurrentUserauthheader(String authHeader, UserUpdateDTO dto) {
        String token = authHeader.replace("Bearer ", "");
        String keycloakId = JwtDecoder.getKeycloakId(token);

        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found with keycloakId: " + keycloakId));

        dto.applyTo(user);
        user.setLastLoginAt(LocalDateTime.now());
        user = userRepository.save(user);

        return new UserUpdateDTO(
                user.getUsername(), user.getEmailAddress(), user.getPoints(),
                user.getPhoneNumber(), user.getBirthdate(), user.getCity(),
                user.getImage(), user.getAboutMe(), user.getRole());
    }

    @Override
    @Transactional
    public User createUser(User user) {
        userRepository.findByUsername(user.getUsername())
                .ifPresent(u -> {
                    throw new UserAlreadyExistsException("Username already exists");
                });
        userRepository.findByEmailAddress(user.getEmailAddress())
                .ifPresent(u -> {
                    throw new UserAlreadyExistsException("Email already exists");
                });

        user.setCreatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        user.setActive(true); // Fixed: Use setActive instead of setIsActive
        User savedUser = userRepository.save(user);

        // Create and link PlayerProfile
        PlayerProfile profile = getOrCreatePlayerProfile(savedUser.getId());
        savedUser.setPlayerProfileId(profile.getId());
        userRepository.save(savedUser);

        return savedUser;
    }

    @Override
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailAddress(email);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public List<User> findAllActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    @Override
    @Transactional
    public UserUpdateDTO updateCurrentUser(String id, UserUpdateDTO updateDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        updateDTO.applyTo(user);
        user = userRepository.save(user);

        return new UserUpdateDTO(
                user.getUsername(), user.getEmailAddress(), user.getPoints(),
                user.getPhoneNumber(), user.getBirthdate(), user.getCity(),
                user.getImage(), user.getAboutMe(), user.getRole());
    }

    @Override
    @Transactional
    public void deleteUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deactivateUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(false); // Fixed: Use setActive instead of setIsActive
        userRepository.save(user);
    }

    @Override
    public List<User> getLeaderboard(int limit) {
        List<User> users = userRepository.findByPointsGreaterThanEqual(0);
        return users.stream()
                .sorted((u1, u2) -> Integer.compare(u2.getPoints(), u1.getPoints()))
                .limit(limit)
                .toList();
    }

    @Override
    @Transactional
    public void updateUserPoints(String userId, int points) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPoints(user.getPoints() + points);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public PlayerProfile getOrCreatePlayerProfile(String userId) {
        return playerProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PlayerProfile profile = PlayerProfile.builder()
                            .userId(userId)
                            .totalCoins(0)
                            .highestScore(0)
                            .gamesPlayed(0)
                            .gamesWon(0)
                            .gameStats(PlayerGameStats.builder()
                                    .totalGamesPlayed(0)
                                    .totalGamesWon(0)
                                    .classicGamesPlayed(0)
                                    .classicGamesWon(0)
                                    .rpgGamesPlayed(0)
                                    .rpgGamesWon(0)
                                    .highestRPGRound(0)
                                    .totalRPGScore(0)
                                    .winRate(0.0)
                                    .currentStreak(0)
                                    .build())
                            .createdAt(LocalDateTime.now())
                            .lastUpdated(LocalDateTime.now())
                            .build();
                    PlayerProfile savedProfile = playerProfileRepository.save(profile);
                    userRepository.findById(userId).ifPresent(user -> {
                        user.setPlayerProfileId(savedProfile.getId());
                        userRepository.save(user);
                    });
                    return savedProfile;
                });
    }




    @Override
    public Optional<User> findByUserId(String Id) {
        return userRepository.findById(Id);
    }

}