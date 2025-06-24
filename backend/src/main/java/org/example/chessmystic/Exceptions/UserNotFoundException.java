package org.example.chessmystic.Exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("User not found with ID: " + id);
    }

    public UserNotFoundException(String keycloakId) {
        super("User not found with Keycloak ID: " + keycloakId);
    }
}