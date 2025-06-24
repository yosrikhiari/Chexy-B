package org.example.chessmystic.Models.APIContacts.RegistreDTO;

import lombok.*;
import org.example.chessmystic.Models.UserManagement.Role;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class RegisterResponseDTO {
    private String userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private String message;
}