package org.example.chessmystic.Models.APIContacts.UserDTO;

import jakarta.validation.constraints.*;
import lombok.*;
import org.example.chessmystic.Models.UserManagement.Role;
import org.example.chessmystic.Models.UserManagement.User;


import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateDTO {
    @Size(min = 3, max = 50)
    private String username;

    @Email
    private String emailAddress;

    @Min(0)
    private Integer points;

    // Fix: Should be String, not Integer
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
    private String phoneNumber;

    @Past
    private Date birthdate;

    @Size(max = 100)
    private String city;

    private String image;

    @Size(max = 500)
    private String aboutMe;

    private Role role;

    public void applyTo(User user) {
        if (this.username != null) {
            user.setUsername(this.username);
        }
        if (this.emailAddress != null) {
            user.setEmailAddress(this.emailAddress);
        }
        if (this.points != null) {
            user.setPoints(this.points);
        }
        if (this.phoneNumber != null) {
            user.setPhoneNumber(this.phoneNumber);
        }
        if (this.birthdate != null) {
            user.setBirthdate(this.birthdate);
        }
        if (this.city != null) {
            user.setCity(this.city);
        }
        if (this.image != null) {
            user.setImage(this.image);
        }
        if (this.aboutMe != null) {
            user.setAboutMe(this.aboutMe);
        }
        if (this.role != null) {
            user.setRole(this.role);
        }
    }
}