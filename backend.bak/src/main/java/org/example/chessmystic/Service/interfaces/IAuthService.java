package org.example.chessmystic.Service.interfaces;

public interface IAuthService {
    String login(String username, String password);
    boolean changePassword(String userId, String newPassword);
}
