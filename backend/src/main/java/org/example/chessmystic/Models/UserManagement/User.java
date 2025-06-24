package org.example.chessmystic.Models.UserManagement;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.chessmystic.Models.Stats.PlayerGameStats;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString(exclude = {"gameStats"}) // Avoid potential circular references in toString
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    @NotNull
    private String keycloakId;

    @Indexed
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Size(max = 50)
    private String firstName;

    @NotBlank
    @Size(max = 50)
    private String lastName;

    @Indexed(unique = true)
    @Email
    @NotNull
    private String emailAddress;

    @Min(0)
    private int points = 0;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
    private String phoneNumber;

    @Past
    private Date birthdate;

    @Size(max = 100)
    private String city;

    private String image;

    @Size(max = 500)
    private String aboutMe;

    @NotNull
    private Role role = Role.USER;

    private String playerProfileId;

    private PlayerGameStats gameStats;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime lastLoginAt;

    private boolean isActive = true;
}