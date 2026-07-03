package com.jake.studysync.service;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.auth.InvalidCredentialsException;
import com.jake.studysync.dto.AuthRequest;
import com.jake.studysync.dto.RegisterRequest;
import com.jake.studysync.exception.UserAlreadyExistsException;
import com.jake.studysync.model.User;
import com.jake.studysync.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private AuthRequest     authRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        authRequest = new AuthRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("password123");
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any( User.class))).thenReturn( createUser());

        var result = authService.register(registerRequest);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(registerRequest.getEmail());
        assertThat(result.getUsername()).isEqualTo(registerRequest.getUsername());
    }

    @Test
    void shouldThrowExceptionWhenEmailExists() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf( UserAlreadyExistsException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldThrowExceptionWhenInvalidCredentials() {
        when(userRepository.findByEmail(authRequest.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(authRequest))
                .isInstanceOf( InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    private User createUser() {
        return User.builder()
                .email(registerRequest.getEmail())
                .username(registerRequest.getUsername())
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .build();
    }
}