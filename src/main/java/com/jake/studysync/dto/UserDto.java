package com.jake.studysync.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDto {
    private UUID id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String profilePictureUrl;
    private Set<String> roles;
}