package com.jake.studysync.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jake.studysync.dto.AuthResponse;
import com.jake.studysync.dto.UserDto;
import com.jake.studysync.event.UserEvent;
import com.jake.studysync.kafka.UserEventProducer;
import com.jake.studysync.model.Role;
import com.jake.studysync.model.User;
import com.jake.studysync.repository.RefreshTokenRepository;
import com.jake.studysync.repository.UserRepository;
import com.jake.studysync.service.CustomUserDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserEventProducer      userEventProducer;
    private final UserRepository         userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper           objectMapper;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        log.info("OAuth2 authentication successful");

        if (authentication.getPrincipal() instanceof CustomOAuth2User oauth2User) {
            handleOAuth2User(request, response, oauth2User);
        } else if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            handleOAuth2User(request, response, oauth2User);
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    private void handleOAuth2User(HttpServletRequest request, HttpServletResponse response,
                                  OAuth2User oauth2User) throws IOException {

        User user = extractUser( oauth2User);

        if (user == null) {
            log.error("User not found in OAuth2 principal");
            response.sendRedirect(frontendUrl + "/login?error=user_not_found");
            return;
        }

        // Generate JWT tokens
        CustomUserDetails userDetails = new CustomUserDetails( user);
        String            accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Send login event
        userEventProducer.sendUserEvent( UserEvent.builder()
                                                .userId(user.getId().toString())
                                                .email(user.getEmail())
                                                .eventType(UserEvent.EventType.LOGIN)
                                                .timestamp(LocalDateTime.now())
                                                .source("OAUTH2")
                                                .build());

        // Cache user session
        redisTemplate.opsForValue().set(
                "session:" + user.getEmail(),
                user,
                java.time.Duration.ofMinutes(15)
        );

        // Prepare response
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(900L)
                .user(convertToDto(user))
                .build();

        // Redirect to frontend with tokens
        String redirectUrl = String.format(
                "%s/oauth2/callback?accessToken=%s&refreshToken=%s&user=%s",
                frontendUrl,
                accessToken,
                refreshToken,
                objectMapper.writeValueAsString(convertToDto(user))
        );

        response.sendRedirect(redirectUrl);
        log.info("OAuth2 login successful for user: {}", user.getEmail());
    }

    private User extractUser(OAuth2User oauth2User) {
        if (oauth2User instanceof CustomOAuth2User customUser) {
            return customUser.getUser();
        }

        // Try to find user by email from OAuth2 attributes
        String email = (String) oauth2User.getAttributes().get("email");
        if (email != null) {
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    private UserDto convertToDto( User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .roles(user.getRoles().stream()
                               .map( Role::getName )
                               .collect(Collectors.toSet()))
                .build();
    }
}