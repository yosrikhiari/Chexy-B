package org.example.chessmystic.Models.APIContacts.UserDTO;


import lombok.*;

@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequestDTO {
    // Getters and setters
    private String userId;
    private String newPassword;
}