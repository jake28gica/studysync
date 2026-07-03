package com.jake.studysync.service;

import com.jake.studysync.dto.AuthRequest;
import com.jake.studysync.dto.AuthResponse;
import com.jake.studysync.dto.RegisterRequest;
import com.jake.studysync.dto.UserDto;
import com.jake.studysync.event.UserEvent;
import com.jake.studysync.exception.InvalidCredentialsException;
import com.jake.studysync.exception.UserAlreadyExistsException;
import com.jake.studysync.exception.UserNotFoundException;
import com.jake.studysync.kafka.UserEventProducer;
import com.jake.studysync.model.AuthProvider;
import com.jake.studysync.model.RefreshToken;
import com.jake.studysync.model.Role;
import com.jake.studysync.model.User;
import com.jake.studysync.repository.RefreshTokenRepository;
import com.jake.studysync.repository.RoleRepository;
import com.jake.studysync.repository.UserRepository;
import com.jake.studysync.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository         userRepository;
    private final RoleRepository         roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JwtService                    jwtService;
    private final UserEventProducer             userEventProducer;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public UserDto register( RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException( "User with email " + request.getEmail() + " already exists");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username " + request.getUsername() + " is already taken");
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .provider( AuthProvider.LOCAL)
                .enabled(true)
                .build();

        // Assign default role
        Role userRole = roleRepository.findByName( "ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));
        user.setRoles(Set.of(userRole));

        User savedUser = userRepository.save(user);

        // Send registration event
        userEventProducer.sendUserEvent( UserEvent.builder()
                                                .userId(savedUser.getId().toString())
                                                .email(savedUser.getEmail())
                                                .eventType(UserEvent.EventType.REGISTERED)
                                                .timestamp(LocalDateTime.now())
                                                .build());

        log.info("User registered successfully: {}", savedUser.getEmail());
        return convertToDto(savedUser);
    }

    @Transactional
    public AuthResponse login( AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException( "Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException("User account is disabled");
        }

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken( refreshToken)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new InvalidCredentialsException("Refresh token has expired");
        }

        User user = token.getUser();
        refreshTokenRepository.delete(token);

        return generateAuthResponse(user);
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        // Blacklist access token
        jwtService.blacklistToken(accessToken);

        // Delete refresh token
        if (refreshToken != null) {
            refreshTokenRepository.deleteByToken(refreshToken);
        }

        // Clear user session from Redis
        String username = jwtService.extractUsername(accessToken);
        redisTemplate.delete("session:" + username);

        // Send logout event
        User user = userRepository.findByEmail(username).orElse(null);
        if (user != null) {
            userEventProducer.sendUserEvent(UserEvent.builder()
                                                    .userId(user.getId().toString())
                                                    .email(user.getEmail())
                                                    .eventType(UserEvent.EventType.LOGOUT)
                                                    .timestamp(LocalDateTime.now())
                                                    .build());
        }

        log.info("User logged out: {}", username);
    }

    @Cacheable(value = "user", key = "#userId")
    public UserDto getUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException( "User not found with id: " + userId));
        return convertToDto(user);
    }

    @CacheEvict(value = "user", key = "#userId")
    @Transactional
    public UserDto updateUser(UUID userId, UserDto userDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());

        User updatedUser = userRepository.save(user);

        // Send update event
        userEventProducer.sendUserEvent(UserEvent.builder()
                                                .userId(updatedUser.getId().toString())
                                                .email(updatedUser.getEmail())
                                                .eventType(UserEvent.EventType.UPDATED)
                                                .timestamp(LocalDateTime.now())
                                                .build());

        log.info("User updated: {}", updatedUser.getEmail());
        return convertToDto(updatedUser);
    }

    private AuthResponse generateAuthResponse(User user) {
        // Generate tokens
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Save refresh token
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(token);

        // Cache user session
        redisTemplate.opsForValue().set(
                "session:" + user.getEmail(),
                user,
                java.time.Duration.ofMinutes(15)
        );

        // Send login event
        userEventProducer.sendUserEvent(UserEvent.builder()
                                                .userId(user.getId().toString())
                                                .email(user.getEmail())
                                                .eventType(UserEvent.EventType.LOGIN)
                                                .timestamp(LocalDateTime.now())
                                                .build());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(900L)
                .user(convertToDto(user))
                .build();
    }

    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .roles(user.getRoles().stream()
                               .map(Role::getName)
                               .collect(Collectors.toSet()))
                .build();
    }
}