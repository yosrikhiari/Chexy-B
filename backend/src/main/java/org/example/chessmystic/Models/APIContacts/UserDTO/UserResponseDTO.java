package org.example.chessmystic.Models.APIContacts.UserDTO;

import lombok.*;
import org.example.chessmystic.Models.UserManagement.Role;
import org.example.chessmystic.Models.UserManagement.User;


@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    public UserResponseDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmailAddress();
        this.role = user.getRole();
    }
}
