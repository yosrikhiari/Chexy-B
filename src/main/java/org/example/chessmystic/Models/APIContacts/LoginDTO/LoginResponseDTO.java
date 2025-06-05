package org.example.chessmystic.Models.APIContacts.LoginDTO;

import lombok.Builder;
import lombok.Data;
import org.example.chessmystic.Models.APIContacts.UserDTO.UserResponseDTO;

@Data
@Builder
public class LoginResponseDTO {
    private String token;
    private String message;
    private UserResponseDTO user;

}