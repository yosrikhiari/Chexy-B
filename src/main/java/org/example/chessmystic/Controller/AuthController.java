package org.example.chessmystic.Controller;

import jakarta.validation.Valid;
import org.example.chessmystic.Models.APIContacts.LoginDTO.LoginRequestDTO;
import org.example.chessmystic.Models.APIContacts.LoginDTO.LoginResponseDTO;
import org.example.chessmystic.Models.APIContacts.RegistreDTO.RegisterRequestDTO;
import org.example.chessmystic.Models.APIContacts.RegistreDTO.RegisterResponseDTO;
import org.example.chessmystic.Exceptions.UserAlreadyExistsException;
import org.example.chessmystic.Service.interfaces.IAuthService;
import org.example.chessmystic.Service.interfaces.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private IUserService userService;

    @Autowired
    private IAuthService authService;


    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO requestDTO) {
        try {
            RegisterResponseDTO response = userService.registerUser(requestDTO);
            return ResponseEntity.ok().body(response);
        } catch (UserAlreadyExistsException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Registration failed", "message", e.getMessage()));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO requestDTO) {
        try{
            LoginResponseDTO response = userService.loginUser(requestDTO);
            return ResponseEntity.ok().body(response);
        } catch (Exception e){
            return ResponseEntity.status(500).body(Map.of("error", "Login failed", "message", e.getMessage()));
        }
    }


    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String newPassword = request.get("newPassword");

        try {
            boolean success = authService.changePassword(userId, newPassword);
            if (success) {
                return ResponseEntity.ok().body(Map.of("message", "Password changed successfully"));
            } else {
                return ResponseEntity.status(400).body(Map.of("error", "Password change failed"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Password change failed", "message", e.getMessage()));
        }
    }

}