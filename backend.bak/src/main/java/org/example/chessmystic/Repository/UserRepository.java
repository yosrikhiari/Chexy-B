package org.example.chessmystic.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.example.chessmystic.Models.UserManagement.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> { // Changed Long to String
    Optional<User> findByKeycloakId(String keycloakId);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmailAddress(String emailAddress);
    List<User> findByIsActiveTrue();

    @Query("{'points': {$gte: ?0}}")
    List<User> findByPointsGreaterThanEqual(int points);

    @Query("{'role': ?0}")
    List<User> findByRole(String role);
}