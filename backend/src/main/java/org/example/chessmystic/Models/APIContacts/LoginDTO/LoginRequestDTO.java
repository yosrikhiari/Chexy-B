package org.example.chessmystic.Models.APIContacts.LoginDTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDTO {

    @NotBlank
    @Email
    private String emailAddress;

    public @NotBlank String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank String password) {
        this.password = password;
    }

    public @NotBlank @Email String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(@NotBlank @Email String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @NotBlank
    private String password;
}
