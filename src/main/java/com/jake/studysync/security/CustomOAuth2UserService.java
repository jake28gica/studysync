package com.jake.studysync.security;

import com.jake.studysync.model.AuthProvider;
import com.jake.studysync.model.User;
import com.jake.studysync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return processOAuth2User(userRequest, oAuth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = extractEmail(provider, attributes);
        String providerId = extractProviderId(provider, attributes);
        String name = extractName(provider, attributes);

        Optional<User> userOptional = userRepository.findByProviderAndProviderId(
                AuthProvider.valueOf( provider), providerId);

        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            // Update user info if needed
            user.setEmail(email);
            user.setFirstName(extractFirstName(name));
            user.setLastName(extractLastName(name));
            user.setProfilePictureUrl(extractProfilePicture(provider, attributes));
            user = userRepository.save(user);
        } else {
            // Check if user exists with same email
            Optional<User> existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent()) {
                user = existingUser.get();
                // Link OAuth2 account to existing user
                user.setProvider(AuthProvider.valueOf(provider));
                user.setProviderId(providerId);
                user = userRepository.save(user);
            } else {
                user = createNewUser(provider, providerId, email, name, attributes);
            }
        }

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    private User createNewUser(String provider, String providerId, String email, String name,
                               Map<String, Object> attributes) {
        User user = User.builder()
                .email(email)
                .username(generateUsername(email))
                .firstName(extractFirstName(name))
                .lastName(extractLastName(name))
                .provider(AuthProvider.valueOf(provider))
                .providerId(providerId)
                .profilePictureUrl(extractProfilePicture(provider, attributes))
                .enabled(true)
                .build();

        return userRepository.save(user);
    }

    private String extractEmail(String provider, Map<String, Object> attributes) {
        return switch (provider) {
            case "GOOGLE" -> (String) attributes.get("email");
            case "FACEBOOK" -> (String) attributes.get("email");
            default -> (String) attributes.get("email");
        };
    }

    private String extractProviderId(String provider, Map<String, Object> attributes) {
        return switch (provider) {
            case "GOOGLE" -> (String) attributes.get("sub");
            case "FACEBOOK" -> (String) attributes.get("id");
            case "TWITTER" -> {
                Map<String, Object> data = (Map<String, Object>) attributes.get("data");
                yield data != null ? (String) data.get("id") : null;
            }
            default -> null;
        };
    }

    private String extractName(String provider, Map<String, Object> attributes) {
        return switch (provider) {
            case "GOOGLE" -> (String) attributes.get("name");
            case "FACEBOOK" -> (String) attributes.get("name");
            case "TWITTER" -> {
                Map<String, Object> data = (Map<String, Object>) attributes.get("data");
                yield data != null ? (String) data.get("name") : null;
            }
            default -> null;
        };
    }

    private String extractFirstName(String fullName) {
        if (fullName == null) return null;
        String[] parts = fullName.split(" ");
        return parts.length > 0 ? parts[0] : fullName;
    }

    private String extractLastName(String fullName) {
        if (fullName == null) return null;
        String[] parts = fullName.split(" ");
        return parts.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length)) : "";
    }

    private String extractProfilePicture(String provider, Map<String, Object> attributes) {
        return switch (provider) {
            case "GOOGLE" -> (String) attributes.get("picture");
            case "FACEBOOK" -> (String) attributes.get("picture");
            default -> null;
        };
    }

    private String generateUsername(String email) {
        String base = email.split("@")[0];
        int counter = 1;
        String username = base;
        while (userRepository.existsByUsername(username)) {
            username = base + counter++;
        }
        return username;
    }
}