package org.example.chessmystic.Models.APIContacts.LoginDTO;

import lombok.*;
import org.example.chessmystic.Models.APIContacts.UserDTO.UserResponseDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class LoginResponseDTO {
    private String token;
    private String message;
    private UserResponseDTO user;

}