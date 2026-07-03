package com.jake.studysync.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        log.error("OAuth2 authentication failed: {}", exception.getMessage());

        // Log the specific error
        String errorMessage = exception.getMessage();
        if (errorMessage != null && errorMessage.contains("User denied access")) {
            log.warn("User denied OAuth2 access");
            response.sendRedirect(frontendUrl + "/login?error=access_denied");
        } else if (errorMessage != null && errorMessage.contains("email not found")) {
            log.warn("Email not found in OAuth2 response");
            response.sendRedirect(frontendUrl + "/login?error=email_not_found");
        } else {
            response.sendRedirect(frontendUrl + "/login?error=oauth2_failure");
        }

        // Clear any security context
        request.getSession().invalidate();
    }
}